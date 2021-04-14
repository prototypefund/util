package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.topobyte.osm4j.core.model.util.OsmModelUtil;
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

		Path dirData = SystemPaths.CWD.resolve("data");
		Path dirKreise = dirData.resolve("kreise");

		RegionExtractor regionExtractor = new RegionExtractor(input, false);
		regionExtractor.prepare();
		regionExtractor.extract(dirKreise, tags -> {
			String adminLevel = tags.get("admin_level");
			if (!"6".equals(adminLevel)) {
				return false;
			}
			return true;
		}, entity -> {
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(entity);
			String name = tags.get("name");
			return String.format("%s.smx", name);
		});
	}

}
