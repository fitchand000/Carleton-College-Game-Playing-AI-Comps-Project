import os

def run_server():
    os.system('java -jar ../build/libs/JSettlersServer-2.4.10.jar')

def initialize_new_bot(bot_name):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}"'.format(bot_name=bot_name))

initialize_new_bot('evo bot 1')

