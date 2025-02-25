// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.vcs.log.graph.impl.print;

import com.intellij.util.containers.SLRUMap;
import com.intellij.vcs.log.graph.api.EdgeFilter;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@ApiStatus.Internal
public class EdgesInRowGenerator {
  private static final int CACHE_SIZE = 10;
  private static final int BLOCK_SIZE = 40;

  private final int WALK_SIZE;

  private final @NotNull LinearGraph myGraph;

  private final @NotNull SLRUMap<Integer, GraphEdges> cacheNU = new SLRUMap<>(CACHE_SIZE, CACHE_SIZE * 2);
  private final @NotNull SLRUMap<Integer, GraphEdges> cacheND = new SLRUMap<>(CACHE_SIZE, CACHE_SIZE * 2);

  public EdgesInRowGenerator(@NotNull LinearGraph graph) {
    this(graph, 1000);
  }

  public EdgesInRowGenerator(@NotNull LinearGraph graph, int walk_size) {
    myGraph = graph;
    WALK_SIZE = walk_size;
  }

  public @NotNull Set<GraphEdge> getEdgesInRow(int rowIndex) {
    GraphEdges neighborU = getNeighborU(rowIndex);
    while (neighborU.myRow < rowIndex) {
      neighborU = oneDownStep(neighborU);
    }

    GraphEdges neighborD = getNeighborD(rowIndex);
    while (neighborD.myRow > rowIndex) {
      neighborD = oneUpStep(neighborD);
    }

    Set<GraphEdge> result = neighborU.myEdges;
    result.addAll(neighborD.myEdges);
    return result;
  }

  public void invalidate() {
    cacheNU.clear();
    cacheND.clear();
  }

  private @NotNull GraphEdges getNeighborU(int rowIndex) {
    int upNeighborIndex = getUpNeighborIndex(rowIndex);
    GraphEdges graphEdges = cacheNU.get(upNeighborIndex);
    if (graphEdges == null) {
      graphEdges = getUCorrectEdges(upNeighborIndex);
      cacheNU.put(upNeighborIndex, graphEdges);
    }
    return graphEdges.copyInstance();
  }

  private @NotNull GraphEdges getNeighborD(int rowIndex) {
    int downNeighborIndex = getUpNeighborIndex(rowIndex) + BLOCK_SIZE;

    if (downNeighborIndex >= myGraph.nodesCount()) {
      return new GraphEdges(myGraph.nodesCount() - 1);
    }

    GraphEdges graphEdges = cacheND.get(downNeighborIndex);
    if (graphEdges == null) {
      graphEdges = getDCorrectEdges(downNeighborIndex);
      cacheND.put(downNeighborIndex, graphEdges);
    }
    return graphEdges.copyInstance();
  }

  private static int getUpNeighborIndex(int rowIndex) {
    return (rowIndex / BLOCK_SIZE) * BLOCK_SIZE;
  }

  private @NotNull GraphEdges getUCorrectEdges(int rowIndex) {
    int startCalculateIndex = Math.max(rowIndex - WALK_SIZE, 0);
    GraphEdges graphEdges = new GraphEdges(startCalculateIndex);

    for (int i = startCalculateIndex; i < rowIndex; i++) {
      graphEdges = oneDownStep(graphEdges);
    }
    return graphEdges;
  }

  private @NotNull GraphEdges getDCorrectEdges(int rowIndex) {
    int endCalculateIndex = Math.min(rowIndex + WALK_SIZE, myGraph.nodesCount() - 1);
    GraphEdges graphEdges = new GraphEdges(endCalculateIndex);

    for (int i = endCalculateIndex; i > rowIndex; i--) {
      graphEdges = oneUpStep(graphEdges);
    }
    return graphEdges;
  }

  private @NotNull GraphEdges oneDownStep(@NotNull GraphEdges graphEdges) {
    Set<GraphEdge> edgesInCurrentRow = graphEdges.myEdges;
    int currentRow = graphEdges.myRow;

    edgesInCurrentRow.addAll(myGraph.getAdjacentEdges(currentRow, EdgeFilter.NORMAL_DOWN));
    edgesInCurrentRow.removeAll(myGraph.getAdjacentEdges(currentRow + 1, EdgeFilter.NORMAL_UP));

    return new GraphEdges(edgesInCurrentRow, currentRow + 1);
  }

  private @NotNull GraphEdges oneUpStep(@NotNull GraphEdges graphEdges) {
    Set<GraphEdge> edgesInCurrentRow = graphEdges.myEdges;
    int currentRow = graphEdges.myRow;

    edgesInCurrentRow.addAll(myGraph.getAdjacentEdges(currentRow, EdgeFilter.NORMAL_UP));
    edgesInCurrentRow.removeAll(myGraph.getAdjacentEdges(currentRow - 1, EdgeFilter.NORMAL_DOWN));
    return new GraphEdges(edgesInCurrentRow, currentRow - 1);
  }

  private static final class GraphEdges {
    // this must be mutably set
    private final @NotNull Set<GraphEdge> myEdges;
    private final int myRow;

    private GraphEdges(int row) {
      this(new HashSet<>(), row);
    }

    private GraphEdges(@NotNull Set<GraphEdge> edges, int row) {
      myEdges = edges;
      myRow = row;
    }

    @NotNull
    GraphEdges copyInstance() {
      return new GraphEdges(new HashSet<>(myEdges), myRow);
    }
  }
}
