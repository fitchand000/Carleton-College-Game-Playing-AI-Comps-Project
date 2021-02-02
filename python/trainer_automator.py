import json
from old_trainer import *
import os
import re
import shutil
from datetime import datetime

def main(config_file):

    with open(config_file) as f:
        data = json.load(f)


    depth = data['tree_depth']

    for run in data['training_runs']:
        name = run['name']
        dt = datetime.now()
        dt_str = dt.strftime('%d-%m-%Y-%H-%M-%S')

        trainer = Trainer(run['bot_count'], name)
        new_dir = dt_str + '_' + name
        os.makedirs(new_dir)
        os.makedirs(new_dir + '/bots')


        for ts in run['training_sessions']:

            trainer.train(ts['mutation_percent'], ts['generations'], ts['games_per_bot'], ts['fast_count'], ts['bots_per_sim'],
                          ts['operator_probability'], ts['max_children'], ts['constants_only'], ts['selection_percent'],
                          ts['mutation_threshold'], ts['crossover_threshold'])

        trainer.results_to_file(name + '_results', depth)

        files = [f for f in os.listdir('.') if os.path.isfile(f)]

        for f in files:
            if re.search(name + '_\d', f):
                shutil.move(f, os.getcwd() + '/' + new_dir + '/bots')
            elif re.search(name + '_results', f):
                shutil.move(f, os.getcwd() + '/' + new_dir)



if __name__ == "__main__":

    main('sample_config.json')