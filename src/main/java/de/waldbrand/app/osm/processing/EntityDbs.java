package de.waldbrand.app.osm.processing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.topobyte.osm4j.core.resolve.OsmEntityProvider;
import de.topobyte.osm4j.diskstorage.EntityDbSetup;
import de.topobyte.osm4j.diskstorage.EntityProviderImpl;
import de.topobyte.osm4j.diskstorage.nodedb.NodeDB;
import de.topobyte.osm4j.diskstorage.vardb.VarDB;
import de.topobyte.osm4j.diskstorage.waydb.WayRecord;

public class EntityDbs
{
	private Path nodeDbIndex;
	private Path nodeDbData;
	private Path wayDbIndex;
	private Path wayDbData;

	public EntityDbs(Path dir)
	{
		nodeDbIndex = dir.resolve("nodedb.idx");
		nodeDbData = dir.resolve("nodedb.dat");
		wayDbIndex = dir.resolve("waydb.idx");
		wayDbData = dir.resolve("waydb.dat");
	}

	public void init(Path input) throws IOException
	{
		// create entity databases
		if (!Files.exists(nodeDbData)) {
			EntityDbSetup.createNodeDb(input, nodeDbIndex, nodeDbData);
			EntityDbSetup.createWayDb(input, wayDbIndex, wayDbData, false);
		}
	}

	public NodeDB nodeDb() throws FileNotFoundException
	{
		return new NodeDB(nodeDbData, nodeDbIndex);
	}

	public VarDB<WayRecord> wayDb() throws FileNotFoundException
	{
		return new VarDB<>(wayDbData, wayDbIndex, new WayRecord(0));
	}

	public OsmEntityProvider entityProvider() throws FileNotFoundException
	{
		return new EntityProviderImpl(nodeDb(), wayDb());
	}

}
