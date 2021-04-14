package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.xml.sax.SAXException;

import com.slimjars.dist.gnu.trove.set.TLongSet;
import com.slimjars.dist.gnu.trove.set.hash.TLongHashSet;

import de.topobyte.jts.utils.PolygonHelper;
import de.topobyte.osm4j.core.access.OsmIteratorInput;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.utils.FileFormat;
import de.topobyte.osm4j.utils.OsmFileInput;
import de.topobyte.simplemapfile.core.EntityFile;
import de.topobyte.simplemapfile.xml.SmxFileReader;
import de.topobyte.simplemapfile.xml.SmxFileWriter;
import de.topobyte.system.utils.SystemPaths;

public class RegionExtractor
{

	private Path input;
	private boolean useWays;

	private EntityDbs entityDbs;

	private PreparedGeometry prepared;

	public RegionExtractor(Path input, boolean useWays)
	{
		this.input = input;
		this.useWays = useWays;
	}

	public void prepare() throws IOException, ParserConfigurationException,
			SAXException, TransformerException
	{
		Path dir = Files.createTempDirectory("waldbrand-osm");
		entityDbs = new EntityDbs(dir);
		entityDbs.init(input);

		Path dirData = SystemPaths.CWD.resolve("data");
		Path fileBrandenburg = dirData.resolve("Brandenburg.smx");

		// load geometry and prepare for containment tests
		EntityFile brandenburg = SmxFileReader.read(fileBrandenburg);
		Geometry buffer = brandenburg.getGeometry().buffer(0.001);
		prepared = PreparedGeometryFactory.prepare(buffer);
	}

	private GeometryBuilder geometryBuilder = new GeometryBuilder();
	private OsmEntityProvider entityProvider;

	// record way ids used for relation polygon building
	private TLongSet usedWays = new TLongHashSet();

	public void extract(Path dirOutput,
			Function<Map<String, String>, Boolean> selector,
			Function<OsmEntity, String> namer) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		// setup output directory
		Files.createDirectories(dirOutput);

		// iterate data
		geometryBuilder.getRegionBuilder().setIncludePuntal(false);
		geometryBuilder.getRegionBuilder().setIncludeLineal(false);

		entityProvider = entityDbs.entityProvider();

		relations(dirOutput, selector, namer);
		ways(dirOutput, selector, namer);
	}

	private void relations(Path dirOutput,
			Function<Map<String, String>, Boolean> selector,
			Function<OsmEntity, String> namer) throws TransformerException,
			ParserConfigurationException, IOException
	{
		OsmFileInput fileInput = new OsmFileInput(input, FileFormat.TBO);
		OsmIteratorInput iterator = fileInput.createIterator(true, false);
		for (EntityContainer ec : iterator.getIterator()) {
			if (ec.getType() != EntityType.Relation) {
				continue;
			}
			OsmRelation relation = (OsmRelation) ec.getEntity();
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(relation);

			if (!selector.apply(tags)) {
				continue;
			}

			if (useWays) {
				for (OsmRelationMember member : OsmModelUtil
						.membersAsList(relation)) {
					if (member.getType() == EntityType.Way) {
						usedWays.add(member.getId());
					}
				}
			}

			Geometry geometry;
			try {
				geometry = geometryBuilder.build(relation, entityProvider);
			} catch (EntityNotFoundException e) {
				continue;
			}
			if (!prepared.covers(geometry)) {
				continue;
			}
			if (geometry instanceof MultiPolygon) {
				geometry = PolygonHelper
						.unpackMultipolygon((MultiPolygon) geometry);
			}

			write(dirOutput, geometry, relation, tags, namer);
		}
	}

	private void ways(Path dirOutput,
			Function<Map<String, String>, Boolean> selector,
			Function<OsmEntity, String> namer) throws IOException,
			TransformerException, ParserConfigurationException
	{
		OsmFileInput fileInput = new OsmFileInput(input, FileFormat.TBO);
		OsmIteratorInput iterator = fileInput.createIterator(true, false);
		for (EntityContainer ec : iterator.getIterator()) {
			if (ec.getType() != EntityType.Way) {
				continue;
			}
			OsmWay way = (OsmWay) ec.getEntity();
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);

			if (!selector.apply(tags)) {
				continue;
			}
			if (usedWays.contains(way.getId())) {
				continue;
			}

			Geometry geometry;
			try {
				geometry = geometryBuilder.build(way, entityProvider);
			} catch (EntityNotFoundException e) {
				continue;
			}
			if (!prepared.covers(geometry)) {
				continue;
			}

			if (!(geometry instanceof LinearRing)) {
				continue;
			}

			geometry = geometryBuilder.getGeometryFactory()
					.createPolygon((LinearRing) geometry);

			write(dirOutput, geometry, way, tags, namer);
		}
	}

	private void write(Path dirOutput, Geometry geometry, OsmEntity entity,
			Map<String, String> tags, Function<OsmEntity, String> namer)
			throws TransformerException, ParserConfigurationException,
			IOException
	{
		EntityFile entityFile = new EntityFile();
		entityFile.setGeometry(geometry);
		for (Entry<String, String> tag : tags.entrySet()) {
			entityFile.addTag(tag.getKey(), tag.getValue());
		}

		String name = namer.apply(entity);
		Path file = dirOutput.resolve(name);
		SmxFileWriter.write(entityFile, file);
	}

}
