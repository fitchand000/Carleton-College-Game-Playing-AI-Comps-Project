from graphviz import Digraph
import json
import sys
import random

from test_set_up import *
import time

def get_edges(tree, edges, i=1, parent=None):
    temp = tree
    tree = {}
    tree["node%s" % random.randint(10000,99999)] = temp
    name = next(iter(tree.keys()))
    if parent:
        edges.append([parent, name])
    for item in tree[name]:
        element = tree[name][item]
        if type(element) == dict:
            get_edges(element, edges, i+1, parent=name)
        elif type(element) == str:
            edges.append([name, element+str(random.randint(10000,99999))])
    return edges

def reformat(edges):
    d = {}
    for pair in edges:
        lst = []
        value = pair[0]
        for pair in edges:
            if value == pair[0]:
                lst.append(pair[1])
        added = False
        for val in lst:
            if "node" not in val:
                d[value] = val
                added = True
        if not added:
            d[value] = lst[0]
    for edge in edges:
        if "node" in edge[0]:
            edge[0] = d[edge[0]]
        if "node" in edge[1]:
            edge[1] = d[edge[1]]
    new = []
    for edge in edges:
        if not edge[0] == edge[1]:
            new.append(edge)
    edges = new
    for edge in edges:
        if "node" in edge[1]:
            for pair in edges:
                if pair[0] == edge[1]:
                    temp = edge[1]
                    edge[1] = pair[1]
                    pair[1] = temp
    new = []
    for edge in edges:
        if not edge[0] == edge[1]:
            new.append(edge)
    edges = new
    return edges

def main(simulation, bot, generation, display=False):
    with open(bot, "r") as file:
        s = file.read()
    data = json.loads(s)
    edges = []
    edges = get_edges(data, edges)
    edges = reformat(edges)

    graph = Digraph()
    names = []
    for edge in edges:
        if edge[0] not in names:
            names.append(edge[0])
            graph.node(edge[0], edge[0][:-5])
        if edge[1] not in names:
            names.append(edge[1])
            graph.node(edge[1], edge[1][:-5])
    for edge in edges:
        graph.edge(edge[0], edge[1])

    graph.render("tree-output/%s-%s-%s" % (simulation, bot[:-4], generation), view=display)

if __name__ == "__main__":
    main('asdf', 'parameter_update_test_9.txt', '2')


