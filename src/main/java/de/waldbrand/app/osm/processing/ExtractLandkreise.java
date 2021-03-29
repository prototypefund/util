package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.xml.sax.SAXException;

import de.topobyte.jts.utils.PolygonHelper;
import de.topobyte.osm4j.core.access.OsmIteratorInput;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.core.resolve.EntityNotFoundException;
import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.diskstorage.EntityDbSetup;
import de.topobyte.osm4j.diskstorage.EntityProviderImpl;
import de.topobyte.osm4j.diskstorage.nodedb.NodeDB;
import de.topobyte.osm4j.diskstorage.vardb.VarDB;
import de.topobyte.osm4j.diskstorage.waydb.WayRecord;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.utils.FileFormat;
import de.topobyte.osm4j.utils.OsmFileInput;
import de.topobyte.simplemapfile.core.EntityFile;
import de.topobyte.simplemapfile.xml.SmxFileReader;
import de.topobyte.simplemapfile.xml.SmxFileWriter;
import de.topobyte.system.utils.SystemPaths;

public class ExtractLandkreise
{

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		if (args.length != 1) {
			System.out.println("usage: extract-landkreise <osm-file.tbo>");
			System.exit(1);
		}

		Path input = Paths.get(args[0]);
		Path dir = Files.createTempDirectory("waldbrand-osm");
		Path nodeDbIndex = dir.resolve("nodedb.idx");
		Path nodeDbData = dir.resolve("nodedb.dat");
		Path wayDbIndex = dir.resolve("waydb.idx");
		Path wayDbData = dir.resolve("waydb.dat");

		Path dirData = SystemPaths.CWD.resolve("data");
		Path fileBrandenburg = dirData.resolve("Brandenburg.smx");
		Path dirKreise = dirData.resolve("kreise");

		// create entity databases
		if (!Files.exists(nodeDbData)) {
			EntityDbSetup.createNodeDb(input, nodeDbIndex, nodeDbData);
			EntityDbSetup.createWayDb(input, wayDbIndex, wayDbData, false);
		}

		// setup output directory
		Files.createDirectories(dirKreise);

		// load geometry and prepare for containment tests
		EntityFile brandenburg = SmxFileReader.read(fileBrandenburg);
		Geometry buffer = brandenburg.getGeometry().buffer(0.001);
		PreparedGeometry prepared = PreparedGeometryFactory.prepare(buffer);

		// iterate data
		GeometryBuilder geometryBuilder = new GeometryBuilder();
		geometryBuilder.getRegionBuilder().setIncludePuntal(false);
		geometryBuilder.getRegionBuilder().setIncludeLineal(false);

		NodeDB nodeDB = new NodeDB(nodeDbData, nodeDbIndex);
		VarDB<WayRecord> wayDB = new VarDB<>(wayDbData, wayDbIndex,
				new WayRecord(0));
		OsmEntityProvider entityProviderImpl = new EntityProviderImpl(nodeDB,
				wayDB);

		OsmFileInput fileInput = new OsmFileInput(input, FileFormat.TBO);
		OsmIteratorInput iterator = fileInput.createIterator(true, false);
		for (EntityContainer ec : iterator.getIterator()) {
			if (ec.getType() != EntityType.Relation) {
				continue;
			}
			OsmRelation relation = (OsmRelation) ec.getEntity();
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(relation);

			String adminLevel = tags.get("admin_level");
			if (!"6".equals(adminLevel)) {
				continue;
			}

			Geometry geometry;
			try {
				geometry = geometryBuilder.build(relation, entityProviderImpl);
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

			String name = tags.get("name");

			EntityFile entityFile = new EntityFile();
			entityFile.setGeometry(geometry);
			for (Entry<String, String> tag : tags.entrySet()) {
				entityFile.addTag(tag.getKey(), tag.getValue());
			}

			Path file = dirKreise.resolve(String.format("%s.smx", name));
			SmxFileWriter.write(entityFile, file);
		}
	}

}
