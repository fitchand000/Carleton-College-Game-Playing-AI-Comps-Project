import json
from trainer import *
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

        trainer = Trainer(run['bot_count'], name, run['print_rate'], run['initial_bots'])
        new_dir = dt_str + '_' + name
        os.makedirs(new_dir)
        os.makedirs(new_dir + '/bots')

        num_sessions = len(run['training_sessions'])
        for i in range(num_sessions):
            ts = run['training_sessions'][i]


            trainer.train(ts['mutation_percent'], ts['generations'], ts['games_per_bot'], ts['fast_count'], ts['bots_per_sim'],
                          ts['operator_probability'], ts['max_children'], ts['constants_only'], ts['performance_cutoff'],
                          ts['high_performer_sample_rate'], ts['node_penalty'], ts['win_bonus'], last_gen=(i == (num_sessions-1)))

        trainer.results_to_file(name + '_results', depth)

        files = [f for f in os.listdir('.') if os.path.isfile(f)]

        try:
            shutil.move('tree-output', os.getcwd() + '/' + new_dir)
        except:
            print("Something went wrong printing trees")

        for f in files:
            if re.search(name + '_\d', f):
                shutil.move(f, os.getcwd() + '/' + new_dir + '/bots')
            elif re.search(name + '_results', f):
                shutil.move(f, os.getcwd() + '/' + new_dir)



if __name__ == "__main__":

    main('new_trainer_config.json')