import os

def run_server():
    os.system('java -jar ../build/libs/JSettlersServer-2.4.10.jar')

def initialize_new_bot(bot_name):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}"'.format(bot_name=bot_name))

def mutate_bot(bot_name, new_bot_name, operator_prob, max_children, constants_only):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{bot_name}" "{new_bot_name}" "{oper}" "{max_children}" "{constants_only}"'
              .format(bot_name=bot_name, new_bot_name=new_bot_name, oper=operator_prob, max_children=max_children, constants_only=constants_only))

def cross_over(bot1, bot2, new_bot_1, new_bot_2):
    os.system('java -cp ../build/libs/JSettlersServer-2.4.10.jar soc.robot.evolutionaryBot.EvolutionaryBotBrain "{b1}" "{b2}" "{n1}" "{n2}"'.format(b1=bot1, b2=bot2, n1=new_bot_1, n2=new_bot_2))

# initialize_new_bot('bot1')
# initialize_new_bot('bot2')
# initialize_new_bot('bot3')
# initialize_new_bot('bot4')


#mutate_bot("bot1", "bot2", "0", "1", "false")

# cross_over('bot1', 'bot2', 'bot3', 'bot4')


















