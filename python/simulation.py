from os import remove, path, system

evo_package = 'soc.robot.evolutionaryBot.EvolutionaryBotClient'

class Simulation:

    def __init__(self, sim_name, evo_bots, sim_count, fast_count):
        assert 3 >= fast_count >= 0
        assert evo_bots

        self.name = sim_name
        self.fast_count = fast_count
        self.evo_bots = evo_bots
        self.sim_count = sim_count
        self.sim_input_file_name = sim_name + '_input.csv'
        self.sim_res_file_name = sim_name + '_results.txt'
        self.summary_results = None

    def simulate(self):
        self._check_initialization()
        self._create_input_file()
        self._create_output_file()
        system('java -jar ../build/libs/JSettlersServer-2.4.10.jar {i} 8880 50'.format(
            i='-Djsettlers.bots.botgames.total=' + self.sim_input_file_name + ',' + self.sim_res_file_name,
        ))
        self._calculate_results()
        self._clean_up()

    def get_results(self):
        return self.summary_results

    def _calculate_results(self):
        res = {}
        game_counts = {}
        res_file = open(self.sim_res_file_name, 'r')
        for line in res_file:
            line = line.rstrip()
            game_scores = line.split(',')
            for i in range(0, len(game_scores), 2):
                cur_bot = game_scores[i]
                cur_count = game_counts.get(cur_bot, 0)
                game_counts[cur_bot] = cur_count + 1
                cur_score = res.get(cur_bot, 0)
                res[cur_bot] = cur_score + self._calculate_score(game_scores[i + 1])

        for bot in res:
            res[bot] /= game_counts[bot]

        res_file.close()

        self.summary_results = res

    def _calculate_score(self, val):
        val = int(val)

        if val < 10:
            return val
        else:
            return val + 10

    def _create_output_file(self):
        res_file = open(self.sim_res_file_name, 'w')
        res_file.close()

    def _check_initialization(self):
        for bot in self.evo_bots:
            if not path.isfile(bot + '.txt'):
                raise Exception("The Bot's text file has not been initialized")

    def _create_input_file(self):
        if path.isfile(self.sim_input_file_name):
            raise Exception('The input file already exists')

        input_file = open(self.sim_input_file_name, 'w')
        input_file.write('f1,f2,f3,s1,s2,s3\n')

        evo_bots = ['{b},{p}'.format(b=bot, p=evo_package) for bot in self.evo_bots]
        input_file.write(','.join(evo_bots) + '\n')

        opponent_string = self._get_opponent_str()

        for bot in self.evo_bots:
            for game in range(self.sim_count):
                input_file.write(bot + opponent_string + '\n')

        input_file.close()

    def _get_opponent_str(self):
        if self.fast_count == 3:
            return ',f1,f2,f3'
        elif self.fast_count == 2:
            return ',f1,f2,s1'
        elif self.fast_count == 1:
            return ',f1,s1,s2'
        else:
            return ',s1,s2,s3'

    def _clean_up(self):
        if path.isfile(self.sim_input_file_name):
            remove(self.sim_input_file_name)
        if path.isfile(self.sim_res_file_name):
            remove(self.sim_res_file_name)


s = Simulation('simulation_test', ['bot2'], 40, 3)
s.simulate()
print(s.get_results())
