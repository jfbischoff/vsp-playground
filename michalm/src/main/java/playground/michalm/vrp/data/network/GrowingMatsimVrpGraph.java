package playground.michalm.vrp.data.network;

import java.util.*;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;

/**
 * 
 * It consists of ShortestPathsArcs with ShortestPath of any type (Sparse, Full or other) - 
 * type of the ShortestPath depends on the given ArcBuilder.
 * 
 * @author michalm
 */
public class GrowingMatsimVrpGraph
    extends GrowingVRPGraph
    implements MATSimVRPGraph
{
    private final Map<Id, MATSimVertex> linkIdToVertex;


    public GrowingMatsimVrpGraph(ArcBuilder arcBuilder)
    {
        super(arcBuilder);
        linkIdToVertex = new LinkedHashMap<Id, MATSimVertex>();
    }


    @Override
    public MATSimVertex getVertex(Id linkId)
    {
        return linkIdToVertex.get(linkId);
    }


    @Override
    public void addVertex(Vertex vertex)
    {
        MATSimVertex mVertex = (MATSimVertex)vertex;
        Id linkId = mVertex.getLink().getId();

        if (linkIdToVertex.put(linkId, mVertex) != null) {
            throw new RuntimeException("Duplicated vertex for link=" + linkId);
        }

        super.addVertex(mVertex);
    }


    @Override
    public ShortestPath getShortestPath(Vertex vertexFrom, Vertex vertexTo)
    {
        return ((ShortestPathArc)getArc(vertexFrom, vertexTo)).getShortestPath();
    }
}
