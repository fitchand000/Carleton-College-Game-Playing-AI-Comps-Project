import numpy as np
import matplotlib.pyplot as plt
import csv

games = {}

#dictionaries of games are being created
with open('combined.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            print(row)
            line_count += 1
        else:
            if row[0] not in games.keys():
                games[row[0]] = {}
            line_count += 1
print(games)

#dictionaries of bots are being created inside the game keys
with open('combined.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]] = {}
            line_count += 1
print(games)

#lists of trackers are being created inside the bot key values
with open('combined.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]][row[4]] = {}
            line_count += 1
print(games)

#appending the WinGameETAs into their designated lists
with open('combined.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    line_count = 0
    for row in csv_reader:
        if line_count == 0:
            line_count += 1
        else:
            games[row[0]][row[1]][row[4]][int(row[3])] = int(row[5])
            line_count += 1
print(games)

## visualization plot is created here!!!
for game in games:
    for bot in games[game]:
        for tracker in games[game][bot]:
            plt.plot(games[game][bot][tracker].keys(), games[game][bot][tracker].values(), label= tracker)
            plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', borderaxespad=0.)
        plt.title("WinGameETAs " + "Bot:" + bot + " " + "Game:" + game)
        plt.xlabel("turnNUmber")
        plt.show()

#creating dictionary of trackers for the second type of plots
trackers = {}
for game in games:
    trackers[game] = {}
    for bot in games[game]:
        for tracker in games[game][bot]:
            trackers[game][tracker] = {}
            for botma in games[game].keys():
                trackers[game][tracker][botma] = {}
print(trackers)
#
#creating the dictionaries
for game in games:
    for bot in games[game]:
        for tracker in games[game][bot]:
            trackers[game][tracker][bot] = games[game][bot][tracker]
print(trackers)
#
#second type of visualization is created here!!
for game in trackers:
    for tracker in trackers[game]:
        for bot in trackers[game][tracker]:
            plt.plot(trackers[game][tracker][bot].keys(), trackers[game][tracker][bot].values(), label=bot)
            plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left', borderaxespad=0.)
        plt.title("WinGameETAs" + " Tracker:" + tracker + " " + "Game:" + "" + game)
        plt.xlabel("turnNUmber")
        plt.show()
