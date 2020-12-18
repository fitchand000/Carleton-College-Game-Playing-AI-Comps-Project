import os

def run_server():
    os.system('java -jar ../build/libs/JSettlersServer-2.4.10.jar')

def initialize_new_bot(bot_name):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}"'.format(bot_name=bot_name))

def mutate_bot(bot_name, operator_prob):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}" "{oper}"'.format(bot_name=bot_name, oper=operator_prob))


#initialize_new_bot('bot3')
#run_server()
#mutate_bot("bot2", "50")

# server -> SOCGameHandler -> sendGameStateOver is a good place to look at the end of a game

