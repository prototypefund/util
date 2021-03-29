package de.waldbrand.app.osm.processing;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.topobyte.system.utils.SystemPaths;

public class TestExtractLandkreise
{

	public static void main(String[] args) throws IOException,
			ParserConfigurationException, SAXException, TransformerException
	{
		ExtractLandkreise.main(new String[] { SystemPaths.HOME
				.resolve("git/waldbrand-app/osm-data/Brandenburg.tbo")
				.toString() });
	}

}
