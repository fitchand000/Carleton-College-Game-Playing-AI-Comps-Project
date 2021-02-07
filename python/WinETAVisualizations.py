import numpy as np
import matplotlib.pyplot as plt
import csv

games = {}

with open('simulation_test_results.txt.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            print(row)
            line_count += 1
        else: # let's build the games dictionaries first
            if row[0] not in games.keys():
                games[row[0]] = {}
            line_count += 1

with open('simulation_test_results.txt.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]] = {}
            line_count += 1

with open('simulation_test_results.txt.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]][row[4]] = []
            line_count += 1

with open('simulation_test_results.txt.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]][row[4]].append(int(row[5]))
            line_count += 1
print(games)

from matplotlib.legend_handler import HandlerLine2D

for game in games:
    for bot in games[game]:
        for tracker in games[game][bot]:
            plt.plot(games[game][bot][tracker], label= tracker)
            plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', borderaxespad=0.)
        plt.ylabel("winGameETA of " + "'" + bot + "'" + " in" + "'" + game +"'")
        plt.xlabel("turnNUmber, maybe?")
        plt.show()

trackers = {}
for game in games:
    trackers[game] = {}
    for bot in games[game]:
        for tracker in games[game][bot]:
            trackers[game][tracker] = {}
            for botma in games[game].keys():
                trackers[game][tracker][botma] = []
print(trackers)

for game in games:
    for bot in games[game]:
        for tracker in games[game][bot]:
            trackers[game][tracker][bot] = games[game][bot][tracker]
print(trackers)

for game in trackers:
    for tracker in trackers[game]:
        for bot in trackers[game][tracker]:
            plt.plot(trackers[game][tracker][bot], label= bot)
            plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', borderaxespad=0.)
        plt.ylabel("winGameETA of " + "'" + tracker + "'" + " in" + "'" + game +"'")
        plt.xlabel("turnNUmber, maybe?")
        plt.show()
