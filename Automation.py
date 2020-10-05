import os
import re

# Read in the file
path = os.getcwd()
file_name = path + "/config/agents.xml"
with open(file_name, 'r') as file:
    file_data = file.read()

# Replace the target string
expression_to_replace = re.search(
    r"discount-factor=\"\S*\"", file_data).group(0)


number = .6  # Change this to do different simulations
file_data = file_data.replace(
    expression_to_replace, "discount-factor=\"{:.2f}\"".format(number))

# Write the file out again
with open(file_name, 'w') as file:
    file.write(file_data)
cmd = "java -jar ../logist/logist.jar config/reactive.xml reactive-random"
os.system(cmd)
