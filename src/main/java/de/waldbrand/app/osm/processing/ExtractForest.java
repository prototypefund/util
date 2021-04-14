package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.topobyte.system.utils.SystemPaths;

public class ExtractForest
{

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		if (args.length != 1) {
			System.out.println("usage: extract-forest <osm-file.tbo>");
			System.exit(1);
		}

		Path input = Paths.get(args[0]);

		Path dirData = SystemPaths.CWD.resolve("data");
		Path dirKreise = dirData.resolve("wald");

		RegionExtractor regionExtractor = new RegionExtractor(input);
		regionExtractor.prepare();
		regionExtractor.extract(dirKreise, tags -> {
			String landuse = tags.get("landuse");
			String natural = tags.get("natural");
			return "forest".equals(landuse) || "wood".equals(natural);
		}, entity -> {
			return String.format("relation-%d.smx", entity.getId());
		});
	}

}
