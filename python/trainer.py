from simulation import *
from test_set_up import *
from shutil import copyfile
from print_tree import *
import random
import copy

class Trainer:
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

    def __init__(self, bot_count, bot_prefix='bot', print_rate=0, initialized_bots=[]):
        """
        Initializes all bot files

        bot_count: The number of robots to create (population size)
        mutation_percent: percentage as a number out of 100 of new bots generated via mutation (the rest are generated via crossover)
        bot_prefix: name to use to start the bot files

        print rate = frequency of printing each of the bot trees. Will always print the last generation. Zero won't print any others
        """
        self.bot_prefix = bot_prefix
        self.bot_count = bot_count
        self.results = []
        self.gen_count = 0
        self.print_rate = print_rate

        if not initialized_bots:
            self.bot_names = [self.bot_prefix + '_' + str(i) for i in range(1, self.bot_count + 1)]
            for bot in self.bot_names:
                initialize_new_bot(bot)
        else:
            self.bot_names = [self.bot_prefix + '_' + str(i) for i in range(1, self.bot_count + 1)]
            for i in range(self.bot_count):
                bot_to_use = initialized_bots[i % (len(initialized_bots))]
                new_file_name = self.bot_names[i]
                copyfile(bot_to_use + '.txt', new_file_name + '.txt')

        self.config_count = 0
        self.mutation_percent = []
        self.fast_count = []
        self.games_per_bot = []
        self.total_generations = []

        self.operator_probability = []
        self.max_children = []
        self.constants_only = []
        self.performance_cutoff = []
        self.high_performer_sample_rate = []
        self.node_penalty = []
        self.win_bonus = []

    def results_to_file(self, file_name, depth=-1):
        res_file = open(file_name + '.txt', 'w')

        for i in range(self.config_count):
            res_file.write("New Trainer\nMutation Percent: {m}, Bot Count: {n}, Games per bot: {g}, generation count: {t}, fast count: {f}, Max depth: {d}, Operator Probability: {o}, Max Children: {mc}, Constants Only: {co}, Performance cutoff: {pc}, High Performance Sample Rate: {hp}, Node Penalty: {np}, Win Bonus: {wb}\n".format(
                m=self.mutation_percent[i], n=self.bot_count, g=self.games_per_bot[i], f=self.fast_count[i], t=self.total_generations[i], d=depth, o=self.operator_probability[i], mc=self.max_children[i], co=self.constants_only[i], pc=self.performance_cutoff[i], hp=self.high_performer_sample_rate[i], np=self.node_penalty[i], wb=self.win_bonus[i]))
            res_file.write(str(self.results[i]))
            res_file.write('\n\n')
        res_file.close()


    def train(self, mutation_percent, generations, games_per_bot, fast_count, bots_per_sim,
              operator_probability, max_children, constants_only, performance_cutoff,
              high_performer_sample_rate, node_penalty, win_bonus, last_gen=False, delete_files=True):
        """
        mutation_percent: percentage of new bots generated via mutation (the rest are generated via crossover) (0-1)
        generations: number of generations to train for
        games_per_bot: number of games each bot plays each generation
        fast_count: number of fast bots in each simulation (smart bots will be 3 - fast bots)
        bots_per_sim: Number of bots to include in each simulation, default is all bots in one simulation
        operator_probability: probability of making an operator on a mutation (string number from 0 - 100)
        max_children: the maximum number of children the node we are mutating is allowed to have (-1 for any number of children)
        constants_only: boolean, only mutate constant values in the tree
        """
        self.config_count += 1

        self.total_generations.append(generations)
        self.mutation_percent.append(mutation_percent)
        self.games_per_bot.append(games_per_bot)
        self.fast_count.append(fast_count)
        self.operator_probability.append(operator_probability)
        self.max_children.append(max_children)
        self.constants_only.append(constants_only)
        self.performance_cutoff.append(performance_cutoff)
        self.high_performer_sample_rate.append(high_performer_sample_rate)
        self.node_penalty.append(node_penalty)
        self.win_bonus.append(win_bonus)

        self.results.append({})
        cur_scores = self.results[-1]


        for gen in range(generations):
            print('starting generation:', self.gen_count)
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
                               games_per_bot, fast_count, node_penalty=node_penalty, delete_files=delete_files, time_out='200s', retry_count=10, win_bonus_score=win_bonus))

            if self.bot_count % bots_per_sim != 0:
                bots_left_over = self.bot_count - (simulation_count * bots_per_sim)
                final_sim_name = self.bot_prefix + "_generation_" + str(self.gen_count) + '_' + str(
                    simulation_count + 1)
                simulations.append(
                    Simulation(final_sim_name, self.bot_names[-bots_left_over:], games_per_bot, fast_count,
                               node_penalty=node_penalty, delete_files=delete_files, time_out='200s', retry_count=10, win_bonus_score=win_bonus))

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
                self._print_all_bots()
                return

            if self.print_rate > 0 and (self.gen_count == 1 or self.gen_count % self.print_rate == 0):
                self._print_all_bots()

            # Splits bots into high performers and low performers
            gen_results = [(k, cur_scores[self.gen_count][k]) for k in cur_scores[self.gen_count]]
            total_fitness = sum(x[1] for x in gen_results)
            fitness_cutoff = performance_cutoff * total_fitness
            gen_results.sort(key=lambda x: x[1], reverse=True) #sorting high to low
            fitness_accumulator = 0
            current_tree = 0
            while fitness_accumulator <= fitness_cutoff:
                fitness_accumulator += gen_results[current_tree][1]
                current_tree+=1
            high_performers = gen_results[:current_tree]
            low_performers = gen_results[current_tree:]

            # normalize fitness, so random.choices has the right weights
            total_high_fitness = sum(x[1] for x in high_performers)
            total_low_fitness = sum(x[1] for x in low_performers)
            normalized_high_performers = [[a[0], a[1] / float(total_high_fitness)] for a in high_performers]
            normalized_low_performers = [[a[0], a[1] / float(total_low_fitness)] for a in low_performers]

            # calculate number of bots to mutate/cross over. cross over count must be even
            mutate_count = round(mutation_percent * len(gen_results))
            cross_over_count = len(gen_results) - mutate_count
            if cross_over_count % 2 != 0:
                if mutate_count == 0:
                    cross_over_count -= 1
                    mutate_count += 1
                else:
                    cross_over_count += 1
                    mutate_count -= 1

            # allow for storing new bots in previous gen_results
            # now includes copying files
            normalized_high_performers = copy.deepcopy(normalized_high_performers)
            normalized_low_performers = copy.deepcopy(normalized_low_performers)
            for tree in normalized_high_performers:
                file_name = tree[0]
                new_file_name = "COPY" + file_name
                copyfile(file_name + '.txt', new_file_name + '.txt')
                tree[0] = new_file_name
            for tree in normalized_low_performers:
                file_name = tree[0]
                new_file_name = "COPY" + file_name
                copyfile(file_name + '.txt', new_file_name + '.txt')
                tree[0] = new_file_name

            # mutate bots
            for i in range(mutate_count):
                # pick whether to take a high or low performer, then pick one of them, then strip lists
                bot_to_mutate = random.choices([random.choices(normalized_high_performers,
                                                               weights=(a[1] for a in normalized_high_performers)),
                                                random.choices(normalized_low_performers,
                                                               weights=(a[1] for a in normalized_low_performers))],
                                               weights=[high_performer_sample_rate, 1 - high_performer_sample_rate])[0][
                    0][0]
                bot_to_replace = gen_results.pop()[0]  # we shouldn't be looking at gen_results any more
                mutate_bot(bot_to_mutate, bot_to_replace, operator_probability, max_children, constants_only)

            # Cross over bots
            for i in range(cross_over_count // 2):
                bot1_to_cross_over = random.choices([random.choices(normalized_high_performers,
                                                                    weights=(a[1] for a in normalized_high_performers)),
                                                     random.choices(normalized_low_performers,
                                                                    weights=(a[1] for a in normalized_low_performers))],
                                                    weights=[high_performer_sample_rate,
                                                             1 - high_performer_sample_rate])[0][0][0]
                bot2_to_cross_over = random.choices([random.choices(normalized_high_performers,
                                                                    weights=(a[1] for a in normalized_high_performers)),
                                                     random.choices(normalized_low_performers,
                                                                    weights=(a[1] for a in normalized_low_performers))],
                                                    weights=[high_performer_sample_rate,
                                                             1 - high_performer_sample_rate])[0][0][0]
                bot1_to_replace = gen_results.pop()[0]
                bot2_to_replace = gen_results.pop()[0]
                cross_over(bot1_to_cross_over, bot2_to_cross_over, bot1_to_replace, bot2_to_replace)

            # Get rid of orphaned files
            for tree in normalized_high_performers:
                file_name = tree[0]
                remove(file_name + '.txt')
            for tree in normalized_low_performers:
                file_name = tree[0]
                remove(file_name + '.txt')

    def _print_all_bots(self):
        for bot in self.bot_names:
            print_tree('na', bot + '.txt', self.gen_count)


if __name__ == "__main__":

    #t = Trainer(10, 'daniel_trainer', print_rate=2)
    t = Trainer(10, 'new_test', print_rate=2, initialized_bots=['bot2', 'high_crossover_3', 'high_crossover_high_op_7'])
    t.train(mutation_percent=.5, generations=1, games_per_bot=1, fast_count=3, bots_per_sim=10, operator_probability='0',
            max_children='0', constants_only='false', performance_cutoff=.5, high_performer_sample_rate=.8, node_penalty=0.003,
            win_bonus=0, last_gen=True)

    print(t.results)


