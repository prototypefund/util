package de.waldbrand.app.osm.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import de.topobyte.osm4j.core.access.OsmIteratorInput;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.utils.FileFormat;
import de.topobyte.osm4j.utils.OsmFileInput;

public class PrintEmergencyStats
{

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		if (args.length != 1) {
			System.out.println("usage: print-emergency-stats <osm-file.tbo>");
			System.exit(1);
		}

		Path input = Paths.get(args[0]);

		Multiset<OsmTag> counts = HashMultiset.create();

		OsmFileInput fileInput = new OsmFileInput(input, FileFormat.TBO);
		OsmIteratorInput iterator = fileInput.createIterator(true, false);
		for (EntityContainer ec : iterator.getIterator()) {
			OsmEntity entity = ec.getEntity();
			Map<String, String> tags = OsmModelUtil.getTagsAsMap(entity);

			String emergency = tags.get("emergency");
			if (emergency != null) {
				Tag tag = new Tag("emergency", emergency);
				counts.add(tag);
			}
		}
		for (OsmTag tag : Multisets.copyHighestCountFirst(counts)
				.elementSet()) {
			int count = counts.count(tag);
			System.out.println(String.format("* %d: %s", count, tag));
		}
	}

}
