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

        for bot in self.bot_names:
            initialize_new_bot(bot)

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

        for gen in range(generations):
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
                               games_per_bot, fast_count, delete_files))

            if self.bot_count % bots_per_sim != 0:
                bots_left_over = self.bot_count - (simulation_count * bots_per_sim)
                final_sim_name = self.bot_prefix + "_generation_" + str(self.gen_count) + '_' + str(
                    simulation_count + 1)
                simulations.append(
                    Simulation(final_sim_name, self.bot_names[-bots_left_over:], games_per_bot, fast_count,
                               delete_files))

            # run simulations, update results
            self.results[self.gen_count] = {}
            for simulation in simulations:
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


# t = Trainer(8, 'new_bot')
# t.train(mutation_percent=.75, generations=3, games_per_bot=2, fast_count=3, bots_per_sim=4)
# print(t.results)


"""
Results for one experiment with 1 generation:

code:
t = Trainer(8, 'new_bot')
t.train(mutation_percent=.75, generations=1, games_per_bot=2, fast_count=3, bots_per_sim=4)

results: {'new_bot4': 6.0, 'new_bot3': 6.0, 'new_bot1': 5.0, 'new_bot2': 5.5, 'new_bot5': 4.5, 'new_bot8': 6.0, 'new_bot7': 3.5, 'new_bot6': 4.5}

selected = 2,3,4,8
bad = 1,5,6,7

bot1: mutated (by bot 3)
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Ore"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current VP"}},"operator":"\u003c"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Knights To Go"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Road ETA"}},"operator":"*"},"operator":"*"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Ore Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current VP"}},"operator":"/"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resource Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Brick Income"}},"operator":"*"},"operator":"\u003e"},"operator":"*"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Knights To Go"}},"operator":"\u003c"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Wheat"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"operator":"/"},"operator":"*"},"operator":"\u003e"}

bot2: mutated (replaced bot 7)
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Build Location Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Brick"}},"operator":"+"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"operator":"-"},"operator":"+"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Brick Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current VP"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Ready Build Spot Count"}},"operator":"\u003c"},"operator":"\u003c"},"operator":"-"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Build Location Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Brick"}},"operator":"+"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"operator":"-"},"operator":"+"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Brick Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current VP"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Ready Build Spot Count"}},"operator":"\u003c"},"operator":"\u003c"},"operator":"-"}

bot3: mutated (replaced bot 1)
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Knights To Go"}},"operator":"\u003c"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Wheat"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"operator":"/"},"operator":"*"},"operator":"\u003e"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Knights To Go"}},"operator":"\u003c"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Wheat"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"operator":"/"},"operator":"*"},"operator":"\u003e"}

bot4: (crossed over with 8 to replace 5 and 6)
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"operator":"\u003c"},"operator":"-"},"right":{"type":1,"nodeDepth":2,"value":{"inputName":"Log Income"}},"operator":"\u003e"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"operator":"\u003c"},"operator":"-"},"right":{"type":1,"nodeDepth":2,"value":{"inputName":"Log Income"}},"operator":"\u003e"}

bot5: replaced by cross over with 4 and 8
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Time To Longest Road"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"operator":"\u003e"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Dev Card Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"operator":"\u003e"},"operator":"\u003e"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Log Income"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Dev Card Count"}},"operator":"-"},"operator":"-"},"operator":"+"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Road ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"operator":"\u003c"},"operator":"-"},"right":{"type":1,"nodeDepth":2,"value":{"inputName":"Log Income"}},"operator":"\u003e"}

bot6: replaced by cross over with 4 and 8
    old:{"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Ready Build Spot Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Wheat Income"}},"operator":"+"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resource Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Sheep"}},"operator":"/"},"operator":"*"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Settlement ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"operator":"\u003c"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resource Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Ore"}},"operator":"\u003e"},"operator":"\u003e"},"operator":"\u003e"}
    new:{"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"operator":"/"},"operator":"*"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"City ETA"}},"operator":"*"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Time To Longest Road"}},"operator":"+"},"operator":"-"}

bot7: mutated (by bot 2)
    old: {"type":1,"nodeDepth":1,"value":{"inputName":"Current Brick"}}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Build Location Count"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Brick"}},"operator":"+"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Time To Longest Road"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Port Count"}},"operator":"-"},"operator":"+"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Brick Income"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Total Resources"}},"operator":"*"},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current VP"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Ready Build Spot Count"}},"operator":"\u003c"},"operator":"\u003c"},"operator":"-"}

bot8: (crossed over with 4 to replace 5 and 6)
    old: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"operator":"/"},"operator":"*"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Road ETA"}},"operator":"*"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Time To Longest Road"}},"operator":"+"},"operator":"-"}
    new: {"type":0,"nodeDepth":1,"left":{"type":0,"nodeDepth":2,"left":{"type":1,"nodeDepth":3,"value":{"inputName":"Settlement ETA"}},"right":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Largest Army ETA"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Development Card ETA"}},"operator":"/"},"operator":"*"},"right":{"type":0,"nodeDepth":2,"left":{"type":0,"nodeDepth":3,"left":{"type":1,"nodeDepth":4,"value":{"inputName":"Current Log"}},"right":{"type":1,"nodeDepth":4,"value":{"inputName":"Road ETA"}},"operator":"*"},"right":{"type":1,"nodeDepth":3,"value":{"inputName":"Time To Longest Road"}},"operator":"+"},"operator":"-"}




results after training for 3 generations:
{1: {'new_bot4': 4.5, 'new_bot2': 3.5, 'new_bot3': 5.0, 'new_bot1': 5.0, 'new_bot6': 5.0, 'new_bot7': 3.5, 'new_bot8': 4.0, 'new_bot5': 3.5}, 2: {'new_bot4': 12.5, 'new_bot3': 5.0, 'new_bot2': 5.5, 'new_bot1': 4.0, 'new_bot5': 6.5, 'new_bot8': 13.0, 'new_bot6': 4.5, 'new_bot7': 6.5}, 3: {'new_bot1': 11.0, 'new_bot4': 4.0, 'new_bot2': 6.0, 'new_bot3': 6.0, 'new_bot7': 4.5, 'new_bot8': 5.5, 'new_bot6': 7.0, 'new_bot5': 12.5}}

"""
