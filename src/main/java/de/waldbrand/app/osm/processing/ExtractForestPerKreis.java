package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.topobyte.melon.paths.PathUtil;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.system.utils.SystemPaths;

public class ExtractForestPerKreis
{

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		if (args.length != 1) {
			System.out
					.println("usage: extract-forest-per-kreis <osm-file.tbo>");
			System.exit(1);
		}

		Path input = Paths.get(args[0]);

		Path dirData = SystemPaths.CWD.resolve("data");
		Path dirForest = dirData.resolve("wald-kreise");
		Path dirKreise = dirData.resolve("kreise");

		Map<Path, Path> mapping = new HashMap<>();
		List<Path> filesBoundaries = PathUtil.list(dirKreise);
		for (Path fileBoundary : filesBoundaries) {
			String name = fileBoundary.getFileName().toString();
			int index = name.lastIndexOf(".smx");
			name = index < 0 ? name : name.substring(0, index);
			Path output = dirForest.resolve(name);
			mapping.put(fileBoundary, output);
		}

		RegionExtractor regionExtractor = new RegionExtractor(input, mapping,
				true);
		regionExtractor.prepare();
		regionExtractor.extract(tags -> {
			String landuse = tags.get("landuse");
			String natural = tags.get("natural");
			return "forest".equals(landuse) || "wood".equals(natural);
		}, entity -> {
			if (entity.getType() == EntityType.Relation) {
				return String.format("relation-%d.smx", entity.getId());
			} else {
				return String.format("way-%d.smx", entity.getId());
			}
		});
	}

}
