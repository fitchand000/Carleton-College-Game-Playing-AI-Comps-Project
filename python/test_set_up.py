import os

def run_server():
    os.system('java -jar ../build/libs/JSettlersServer-2.4.10.jar')

def initialize_new_bot(bot_name):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}"'.format(bot_name=bot_name))

def mutate_bot(bot_name, new_bot_name, operator_prob):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}" "{new_bot_name}" "{oper}"'.format(bot_name=bot_name, new_bot_name=new_bot_name, oper=operator_prob))

def cross_over(bot1, bot2, new_bot_1, new_bot_2):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{b1}" "{b2}" "{n1}" "{n2}"'.format(b1=bot1, b2=bot2, n1=new_bot_1, n2=new_bot_2))

#initialize_new_bot('bot4')
#mutate_bot("bot4", "bot5", "50")

#cross_over('bot1', 'bot2', 'bot3', 'bot4')


















