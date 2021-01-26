from simulation import *
from test_set_up import *
import random

class Trainer:

    def __init__(self, bot_count, bot_prefix='bot'):
        """
        Initializes all bot files

        bot_count: The number of robots to create (population size). MUST BE EVEN NUMBER
        mutation_percent: percentage as a number out of 100 of new bots generated via mutation (the rest are generated via crossover)
        bot_prefix: name to use to start the bot files
        """
        self.bot_prefix = bot_prefix
        self.bot_count = bot_count
        self.results = {}
        self.gen_count = 0
        self.bot_names = [self.bot_prefix + str(i) for i in range(1, self.bot_count + 1)]
        self.mutation_percent = None
        self.fast_count = None
        self.games_per_bot = None
        self.total_generations = None

        for bot in self.bot_names:
            initialize_new_bot(bot)

    def results_to_file(self, file_name):
        res_file = open(file_name + '.txt', 'w')
        res_file.write("Mutation Percent: {m}, Bot Count: {n}, Games per bot: {g}, generation count: {t}, fast count: {f}, Max depth: 7\n".format(
            m=self.mutation_percent, n=self.bot_count, g=self.games_per_bot, f=self.fast_count, t=self.total_generations))
        res_file.write(str(self.results))
        res_file.close()



    def train(self, mutation_percent, generations, games_per_bot, fast_count, bots_per_sim=0, delete_files=True,
              operator_probability='50'):
        """
        Trains the bots for a set number of generations. Each generation half the bots are selected to move on to the next generation
        and the other half are thrown out. The thrown out bots are replaced by a combination of cross over and mutation on the selected
        bots dictated by the mutation percent parameter.

        mutation_percent: percentage of new bots generated via mutation (the rest are generated via crossover)
        generations: number of generations to train for
        games_per_bot: number of games each bot plays each generation
        fast_count: number of fast bots in each simulation (smart bots will be 3 - fast bots)
        bots_per_sim: Number of bots to include in each simulation, default is all bots in one simulation
        operator_probability = probability of making an operator on a mutation (string number from 0 - 100)
        """
        self.total_generations = generations
        self.mutation_percent = mutation_percent
        self.games_per_bot = games_per_bot
        self.fast_count = fast_count

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
            self.results[self.gen_count] = {}
            x = 0
            for simulation in simulations:
                print('on simulation:', x, 'gen:', gen)
                x += 1
                simulation.simulate()
                res = simulation.get_evo_results()

                for key in res:
                    self.results[self.gen_count][key] = res[key]

            # Selects bots to move on and bots to be overwritten
            gen_results = [(k, self.results[self.gen_count][k]) for k in self.results[self.gen_count]]
            gen_results.sort(key=lambda x: x[1], reverse=True)
            selected_bots = gen_results[:len(gen_results) // 2]
            bad_bots = gen_results[len(gen_results) // 2:]
            random.shuffle(selected_bots)
            random.shuffle(bad_bots)

            # calculate number of bots to mutate/cross over. cross over count must be even
            mutate_count = round(mutation_percent * len(selected_bots))
            cross_over_count = len(selected_bots) - mutate_count
            if cross_over_count % 2 != 0:
                if mutate_count == 0:
                    cross_over_count -= 1
                    mutate_count += 1
                else:
                    cross_over_count += 1
                    mutate_count -= 1

            # mutate bots
            for i in range(mutate_count):
                bot_to_mutate = selected_bots.pop()[0]
                bot_to_replace = bad_bots.pop()[0]
                mutate_bot(bot_to_mutate, bot_to_replace, operator_probability)

            # Cross over bots
            for i in range(cross_over_count // 2):
                bot1_to_cross_over = selected_bots.pop()[0]
                bot2_to_cross_over = selected_bots.pop()[0]
                bot1_to_replace = bad_bots.pop()[0]
                bot2_to_replace = bad_bots.pop()[0]
                cross_over(bot1_to_cross_over, bot2_to_cross_over, bot1_to_replace, bot2_to_replace)


# t = Trainer(50, 'test_2_bot')
# t.train(mutation_percent=.5, generations=60, games_per_bot=20, fast_count=3, bots_per_sim=5)
# t.results_to_file(t.bot_prefix + '_training_results')
