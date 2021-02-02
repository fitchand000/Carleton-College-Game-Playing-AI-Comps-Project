from simulation import *
from test_set_up import *
from shutil import copyfile
import random

class OldTrainer:
    """
    High level overview:

    - Initializes a set of evolutionary bots

    - for each generation
        -  Evenly divides robot games up into the number of simulations we want
        -  simulates the each game and keeps track of the results
        -  A certain number of bots based on the selection threshold persist to the next generation,
            the rest are overwritten via mutation or cross over according to the mutation_percent
        -   The candidates for mutation/cross over are taken randomly from set of bots that performed better
            than the mutation/crossover_threshold, respectively
        - Mutation behaves according to the operator_probability, max_children, and constants_only parameters

    Multiple class to trainer.simulate() can be made in a row in order to change parameters after a fixed number of generations


    """

    def __init__(self, bot_count, bot_prefix='bot'):
        """
        Initializes all bot files

        bot_count: The number of robots to create (population size)
        mutation_percent: percentage as a number out of 100 of new bots generated via mutation (the rest are generated via crossover)
        bot_prefix: name to use to start the bot files
        """
        self.bot_prefix = bot_prefix
        self.bot_count = bot_count
        self.results = []
        self.gen_count = 0
        self.bot_names = [self.bot_prefix + '_' + str(i) for i in range(1, self.bot_count + 1)]

        self.config_count = 0
        self.mutation_percent = []
        self.fast_count = []
        self.games_per_bot = []
        self.total_generations = []

        self.operator_probability = []
        self.max_children = []
        self.constants_only = []
        self.selection_percent = []
        self.mutation_threshold = []
        self.crossover_threshold = []

        for bot in self.bot_names:
            initialize_new_bot(bot)

    def results_to_file(self, file_name, depth=-1):
        res_file = open(file_name + '.txt', 'w')

        for i in range(self.config_count):
            res_file.write("Mutation Percent: {m}, Bot Count: {n}, Games per bot: {g}, generation count: {t}, fast count: {f}, Max depth: {d}, Operator Probability: {o}, Max Children: {mc}, Constants Only: {co}, Selection Percent: {sp}, Mutation Threshold {mt}, Crossover Threshold: {ct}\n".format(
                m=self.mutation_percent[i], n=self.bot_count, g=self.games_per_bot[i], f=self.fast_count[i], t=self.total_generations[i], d=depth, o=self.operator_probability[i], mc=self.max_children[i], co=self.constants_only[i], sp=self.selection_percent[i], mt=self.mutation_threshold[i], ct=self.crossover_threshold[i]))
            res_file.write(str(self.results[i]))
            res_file.write('\n\n')
        res_file.close()



    def train(self, mutation_percent, generations, games_per_bot, fast_count, bots_per_sim,
              operator_probability, max_children, constants_only, selection_percent, mutation_threshold, crossover_threshold, last_gen=False, delete_files=True):
        """
        mutation_percent: percentage of new bots generated via mutation (the rest are generated via crossover) (0-1)
        generations: number of generations to train for
        games_per_bot: number of games each bot plays each generation
        fast_count: number of fast bots in each simulation (smart bots will be 3 - fast bots)
        bots_per_sim: Number of bots to include in each simulation, default is all bots in one simulation
        operator_probability: probability of making an operator on a mutation (string number from 0 - 100)
        max_children: the maximum number of children the node we are mutating is allowed to have (-1 for any number of children)
        constants_only: boolean, only mutate constant values in the tree
        selection_percent: percentage of total to advanced to the next generation (0-1)
        mutation_threshold: percentage of total bots that get to be candidates for mutation (0-1)
        mutation_threshold: percentage of total bots that get to be candidates for crossover (0-1)
        """
        self.config_count += 1

        self.total_generations.append(generations)
        self.mutation_percent.append(mutation_percent)
        self.games_per_bot.append(games_per_bot)
        self.fast_count.append(fast_count)
        self.operator_probability.append(operator_probability)
        self.max_children.append(max_children)
        self.constants_only.append(constants_only)
        self.selection_percent.append(selection_percent)
        self.mutation_threshold.append(mutation_threshold)
        self.crossover_threshold.append(crossover_threshold)

        self.results.append({})
        cur_scores = self.results[-1]


        for gen in range(generations):
            print('starting generation:', gen)
            self.gen_count += 1

            # check to see if we are doing default number of games per simulation
            if bots_per_sim == 0:
                bots_per_sim = self.bot_count

            # set up list of simulations
            simulations = []
            simulation_count = self.bot_count // bots_per_sim
            for i in range(simulation_count):
                sim_name = self.bot_prefix + "_generation_" + str(self.gen_count) + '_' + str(i + 1)
                simulations.append(
                    Simulation(sim_name, self.bot_names[i * bots_per_sim: i * bots_per_sim + bots_per_sim],
                               games_per_bot, fast_count, delete_files=delete_files, time_out='200s', retry_count=10))

            if self.bot_count % bots_per_sim != 0:
                bots_left_over = self.bot_count - (simulation_count * bots_per_sim)
                final_sim_name = self.bot_prefix + "_generation_" + str(self.gen_count) + '_' + str(
                    simulation_count + 1)
                simulations.append(
                    Simulation(final_sim_name, self.bot_names[-bots_left_over:], games_per_bot, fast_count,
                               delete_files=delete_files, time_out='200s', retry_count=10))

            # run simulations, update results
            cur_scores[self.gen_count] = {}
            x = 0
            for simulation in simulations:
                print('on simulation:', x, 'gen:', gen)
                x += 1
                simulation.simulate()
                res = simulation.get_evo_results()

                for key in res:
                    cur_scores[self.gen_count][key] = res[key]

            # Don't want to mutate on final generation
            if gen == generations - 1 and last_gen:
                print("\n\nHERE!!!!!!\n\n")
                return

            # calculate the number of bots selected for next gen, selected for crossover, and selected for mutation
            selected_count = min(round(self.bot_count * selection_percent), self.bot_count - 2)
            selected_for_mutation_count = max(round(self.bot_count * mutation_threshold), 1)
            selected_for_crossover_count = max(round(self.bot_count * crossover_threshold), 2)


            # Selects bots to move on and bots to be overwritten
            gen_results = [(k, cur_scores[self.gen_count][k]) for k in cur_scores[self.gen_count]]
            gen_results.sort(key=lambda st: st[1], reverse=True)
            bad_bots = gen_results[selected_count:]
            mutation_candidates = gen_results[:selected_for_mutation_count]
            crossover_candidates = gen_results[:selected_for_crossover_count]

            # calculate number of bots to mutate/cross over. cross over count must be even
            mutate_count = round(mutation_percent * len(bad_bots))
            cross_over_count = len(bad_bots) - mutate_count
            if cross_over_count % 2 != 0:
                if mutate_count == 0:
                    cross_over_count -= 1
                    mutate_count += 1
                else:
                    cross_over_count += 1
                    mutate_count -= 1

            # Create a copy of mutation candidates
            mutation_file_names = []
            for bot in mutation_candidates:
                bot_name = bot[0]
                new_bot_name = bot_name + "_mutation"
                copyfile(bot_name + '.txt', new_bot_name + '.txt')
                mutation_file_names.append(new_bot_name)

            # mutate bots
            for i in range(mutate_count):
                bot_to_mutate = random.choice(mutation_file_names)
                bot_to_replace = bad_bots.pop()[0]
                mutate_bot(bot_to_mutate, bot_to_replace, operator_probability, max_children, constants_only)

            # Clean up copied files
            for mutation_file in mutation_file_names:
                os.remove(mutation_file + '.txt')

            # Create a copy of crossover candidates
            crossover_file_names = []
            for bot in crossover_candidates:
                bot_name = bot[0]
                new_bot_name = bot_name + "_crossover"
                copyfile(bot_name + '.txt', new_bot_name + '.txt')
                crossover_file_names.append(new_bot_name)

            # Cross over bots
            for i in range(cross_over_count // 2):
                bot1_to_cross_over = random.choice(crossover_file_names)
                bot2_to_cross_over = bot1_to_cross_over
                while bot1_to_cross_over == bot2_to_cross_over :
                    bot2_to_cross_over = random.choice(crossover_file_names)
                bot1_to_replace = bad_bots.pop()[0]
                bot2_to_replace = bad_bots.pop()[0]
                cross_over(bot1_to_cross_over, bot2_to_cross_over, bot1_to_replace, bot2_to_replace)

            # Clean up copied files
            for crossover_file in crossover_file_names:
                os.remove(crossover_file + '.txt')


if __name__ == "__main__":

    t = OldTrainer(40, 'new_tree_structure_test')
    t.train(mutation_percent=.5, generations=50, games_per_bot=30, fast_count=3, bots_per_sim=2, operator_probability='50',
            max_children='-1', constants_only='false', selection_percent=.25, mutation_threshold=.25, crossover_threshold=.25)
    t.train(mutation_percent=.5, generations=5, games_per_bot=30, fast_count=3, bots_per_sim=2,
            operator_probability='40',
            max_children='-1', constants_only='false', selection_percent=.25, mutation_threshold=.25,
            crossover_threshold=.25)
    t.results_to_file(t.bot_prefix + '_training_results', 7)


