import time
from simulation import *
from test_set_up import *



def time_simulation(fast_count, game_count, sim_count):
    """

    :param fast_count: Number of fast robots in the game (3 max), other robots will all be smart robots
    :param game_count: Total number of games you want to run
    :param sim_count: The number of different simulations used to run all the games
    :return: Duration of the experiments
    """
    if game_count < sim_count:
        return -1

    initialize_new_bot('timerbot')

    games_per_sim = game_count // sim_count
    extra_sims = game_count % sim_count
    simulations = []

    for i in range(sim_count):
        if i < extra_sims:
            simulations.append(Simulation('Timer_test_' + str(i), ['timerbot'], games_per_sim + 1, fast_count))
        else:
            simulations.append(Simulation('Timer_test_' + str(i), ['timerbot'], games_per_sim, fast_count))

    s = time.time()
    for simulation in simulations:
        simulation.simulate()
    e = time.time()

    remove('timerbot.txt')

    return e - s


if __name__ == "__main__":
    print('TEST', time_simulation(0, 60, 2))
