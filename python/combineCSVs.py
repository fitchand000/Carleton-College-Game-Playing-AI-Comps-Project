import csv

### creating combined CSV
smartBots = csv.reader(open('simulation_test_results.txt.csv'))
evoBots = csv.reader(open('evo_simulation_test_results.txt.csv'))
f = open("combined.csv", "w")
writer = csv.writer(f)

for row in smartBots:
    writer.writerow(row)

next(evoBots)
for row in evoBots:
    writer.writerow(row)
f.close()