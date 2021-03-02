import matplotlib.pyplot as plt
import csv



def combine_csvs(file_name):
    smartBots = csv.reader(open(file_name))
    evoBots = csv.reader(open('evo_' + file_name))
    new_name = file_name + "_combined.csv"

    f = open(new_name, "w")
    writer = csv.writer(f)

    for row in smartBots:
        writer.writerow(row)

    next(evoBots)
    for row in evoBots:
        writer.writerow(row)
    f.close()
    return new_name



def create_dict(file_name):
    games = {}

    #dictionaries of games are being created
    with open(file_name) as csv_file:
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

    #dictionaries of bots are being created inside the game keys
    with open(file_name) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                line_count += 1
            else:
                games[row[0]][row[1]] = {}
                line_count += 1

    #lists of trackers are being created inside the bot key values
    with open(file_name) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                line_count += 1
            else:
                games[row[0]][row[1]][row[4]] = {}
                line_count += 1

    #appending the WinGameETAs into their designated lists
    with open(file_name) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                line_count += 1
            else:
                games[row[0]][row[1]][row[4]][int(row[3])] = int(row[5])
                line_count += 1

    return games



def plot_eta_for_all_bots(games):
    ## visualization plot is created here!!!
    for game in games:
        for bot in games[game]:
            for tracker in games[game][bot]:
                plt.plot(games[game][bot][tracker].keys(), games[game][bot][tracker].values(), label= tracker)
                plt.legend(bbox_to_anchor=(1.01, 1), loc='upper left', borderaxespad=0.)
            plt.title("Smart-Tree Win ETA Predictions for all Players")
            plt.xlabel("Turn Number")
            plt.ylabel("Win Game ETA")
            plt.show()

def plot_etas_for_one_bot(games):
    trackers = {}
    for game in games:
        trackers[game] = {}
        for bot in games[game]:
            for tracker in games[game][bot]:
                trackers[game][tracker] = {}
                for botma in games[game].keys():
                    trackers[game][tracker][botma] = {}
    #
    #creating the dictionaries
    for game in games:
        for bot in games[game]:
            for tracker in games[game][bot]:
                trackers[game][tracker][bot] = games[game][bot][tracker]
    #
    #second type of visualization is created here!!
    for game in trackers:
        for tracker in trackers[game]:
            for bot in trackers[game][tracker]:
                plt.plot(trackers[game][tracker][bot].keys(), trackers[game][tracker][bot].values(), label=bot)
                plt.legend(bbox_to_anchor=(1.01, 1), loc='upper left', borderaxespad=0.)
            plt.title("WinGameETAs" + " Tracker:" + tracker + " Game: " + game)
            plt.xlabel("turnNUmber")
            plt.show()

if __name__ == '__main__':


    tf = 'test_results.txt.csv'

    f = combine_csvs(tf)
    g = create_dict(f)
    plot_eta_for_all_bots(g)
    plot_etas_for_one_bot(g)