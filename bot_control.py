import asyncio
import websockets
import json
import ast
from openai import OpenAI
import tiktoken

client = OpenAI(api_key = "")
goals = []

auto_goal = False
temp = 0.3

def count_tokens(text):
    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)
    return len(tokens)

async def send_message():
    uri = "ws://localhost:8080"
    async with websockets.connect(uri, ping_interval=None) as websocket:
        iterations = 0
        tokens = 0
        unique_items = set()
        while True:
            response = await websocket.recv()
            response_data = json.loads(response)
            iterations += 1
            print(f"Iteration {iterations}, Number of Tokens {tokens}")
            if response_data['type'] == 'planFailure':
                print(f"Plan failed: {response_data['failureReason']}")
                new_plan, nTokens = query_chatgpt_for_plan_update(goal, "\n".join(initial_plan), response_data['failureReason'],  response_data['inventory'],  response_data['info'])
                print("New plan: " + str(new_plan))
                update_message = {
                    "type": "planUpdate",
                    "body": new_plan
                }
                tokens += nTokens
                await websocket.send(json.dumps(update_message))
            elif response_data['type'] == 'setGoal':
                goal = response_data['body'][0]
                if goal == 'auto':
                    auto_goal = True
                    print("Auto-generating initial goal")
                    goal, nTokens = query_chatgpt_for_new_goal()
                print("Received goal: " + goal)
                initial_plan, nTokens = query_chatgpt_for_initial_plan(goal, response_data['inventory'],  response_data['info'])
                print("Initial plan: " + str(initial_plan))
                initial_message = {
                    "type": "initialPlan",
                    "body": initial_plan
                }
                tokens += nTokens
                await websocket.send(json.dumps(initial_message))
            elif response_data['type'] == 'planSuccess':
                print("Plan succeeded on iteration " + str(iterations))
                print("Number of Tokens Used: " + tokens)
                '''
                if auto_goal:
                    new_goal = query_chatgpt_for_new_goal()
                    new_plan = query_chatgpt_for_initial_plan(new_goal, response_data['inventory'],  response_data['info'])
                    new_message = {
                        "type": "initialPlan",
                        "body": new_plan
                    }
                    await websocket.send(json.dumps(new_message))
                    '''
            else:
                print(f"Received: {response}")

command_prompt = (
    f"Now, generate a series of commands to accomplish the given goal according to a user-provided plan. "
    f"The available commands are:\n"
    f"1. goto_block block_type - go to a block (e.g., goto_block oak_log or goto_block crafting_table).\n"
    f"2. mine amount block_type - mine a block until you have \"amount\" total in your inventory (e.g., mine 64 diamond_ore). The block should be specified by its actual name, not the drop (e.g., use diamond_ore instead of diamond, stone instead of cobblestone).\n"
    f"If blocks are not in view, the player will explore caves by default. "
    f"To mine something not underground (e.g., logs), first goto_block (e.g., goto_block oak_log) and then mine (e.g., mine oak_log).\n"
    f"For common materials (wood, stone), get a little bit more than what is immediately needed, as they are often used in crafting.\n"
    f"3. craft n item_name - craft an item using the inventory or a crafting table until you have n of them. Use the official item name. (e.g., craft 2 stick, craft 2 oak_planks).\n"
    f"4. smelt n item_name - smelt an item using a furnace until you have n of them. (e.g., smelt 2 iron_ingot).\n"
    f"Will fail if the required resources are not present in the player's inventory, or if a crafting table/furnace is not available.\n\n"
    f"5. place block_name - place a block in the world (e.g., place crafting_table, place furnace).\n"
    f"Will fail if the player does not have the block in their inventory.\n"
    f"6. goto_y_level y - attempt to go to a specific y-level in the world (e.g., goto_y_level 12).\n"
    f"Useful to go to the most likely y-level to find an ore before mining.\n\n"
    f"Given the goal: \"{{goal}}\", generate a list of commands to achieve it. Output nothing but the commands, separated by newlines. Do not include special characters to mark the start or end of the list."
)

def query_chatgpt_for_initial_plan(goal, inventory, info):
    system_prompt = (
        f"You serve as an assistant that helps me play Minecraft."
    )
    plan_prompt = (
        f"I will give you my goal in the game, please break it down as a tree-structure plan to achieve this goal. The requirements of the tree-structure plan are:\n"
        f"1. The plan tree should be exactly of depth 2.\n"
        f"2. Describe each step in one line.\n"
        f"3. You should index the two levels like ’1.’, ’1.1.’, ’1.2.’, ’2.’, ’2.1.’, etc.\n"
        f"4. The sub-goals at the bottom level should be basic actions so that I can easily execute them in the game with the resources I have."
        f"5. If I already have something, you don't need to generate steps to acquire it again. You can assume that I have the following items in my inventory: {inventory}. Additionally, {info}"
        f"6. Wood type matters. For example, if I only have birch logs, you should specify birch planks."
        f"Given the goal: \"{goal}\", generate a tree-structure plan to achieve it. Output nothing but the plan."
    )
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": plan_prompt}
        ],
        max_tokens=150,
        temperature = temp
    )
    initial_plan_text = response.choices[0].message.content.strip()
    command_response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": plan_prompt},
            {"role": "assistant", "content": initial_plan_text},
            {"role": "user", "content": command_prompt.format(goal=goal)}
        ],
        max_tokens=150,
        temperature = temp
    )
    goals.append(goal)
    input_tokens = count_tokens(system_prompt)*2 + count_tokens(plan_prompt)*2 + count_tokens(initial_plan_text) + count_tokens(command_prompt.format(goal=goal))
    output_tokens = count_tokens(initial_plan_text) + count_tokens(command_response.choices[0].message.content.strip())
    return command_response.choices[0].message.content.strip().split('\n'), input_tokens + output_tokens

def query_chatgpt_for_plan_update(goal, plan, failure_reason, inventory, info):
    system_prompt = (
        f"You serve as an assistant that helps me play Minecraft."
    )
    replan_prompt = (
        f"{failure_reason}. Please reason through why this might have happened, "
        f"starting from the step that failed. Your current inventory is: {inventory}. Additionally, {info}"
    )
    recommand_prompt = (
        f"Now, generate a series of commands to finish accomplishing the given goal according to the new plan. "
        f"1. If smelting or crafting times out, try surfacing to find a valid place to put the block."
        f"2. If the error is that there is not a tool to mine the block, craft a suitable tool to mine the block."
        f"3. If crafting fails, make sure you have all the necessary materials in your inventory."
        f"Start from the failed step, do not retrace any previous steps (except possibly a goto_block if the failed step was a mine). Output nothing but the commands, separated by newlines."
    )

    plan_response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": command_prompt.format(goal=goal)},
            {"role": "assistant", "content": plan},
            {"role": "user", "content": replan_prompt}
        ],
        max_tokens=150,
        temperature = temp
    )
    newplan_text = plan_response.choices[0].message.content.strip()
    print("Failure explanation and new plan: " + newplan_text)
    command_response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": command_prompt.format(goal=goal)},
            {"role": "assistant", "content": plan},
            {"role": "user", "content": replan_prompt},
            {"role": "assistant", "content": newplan_text},
            {"role": "user", "content": recommand_prompt}
        ],
        max_tokens=150,
        temperature = temp
    )
    # get number of prompt input tokens
    input_tokens = count_tokens(system_prompt) + count_tokens(command_prompt.format(goal=goal)) + count_tokens(plan) + count_tokens(replan_prompt) + count_tokens(newplan_text) + count_tokens(recommand_prompt)
    output_tokens = count_tokens(newplan_text) + count_tokens(command_response.choices[0].message.content.strip())
    return command_response.choices[0].message.content.strip().split('\n'), input_tokens + output_tokens

def query_chatgpt_for_new_goal():
    system_prompt = (
        f"You serve as an assistant that helps me play Minecraft."
    )
    new_plan_prompt = (
        f"Generate a new goal for the game that is appropriate given the current progress."
        f"1. The goal should be specific and achievable, but still enough to advance the game."
        f"2. Here is a list of past goals: {goals}. If it's empty, give me a good starter task like getting necessary tools."
        f"3. Note that I am in peaceful, so I don't need armor/gear."
        f"4. Don't give any goals that are related to building. Just try to advance the game as fast as possible."
        f"5. Output nothing except for the short goal itself."
    )
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": new_plan_prompt}
        ],
        max_tokens=150,
        temperature = temp
    )
    print("New goal: " + response.choices[0].message.content.strip())
    input_tokens = count_tokens(system_prompt) + count_tokens(new_plan_prompt)
    output_tokens = count_tokens(response.choices[0].message.content.strip())
    return response.choices[0].message.content.strip(), input_tokens + output_tokens

asyncio.run(send_message())
