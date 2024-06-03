import asyncio
import websockets
import json
from openai import OpenAI

client = OpenAI(api_key = "API-KEY-HERE")

async def send_message():
    uri = "ws://localhost:8080"
    async with websockets.connect(uri, ping_interval=None) as websocket:
        while True:
            response = await websocket.recv()
            response_data = json.loads(response)
            if response_data['type'] == 'planFailure':
                print(f"Plan failed: {response_data['failureReason']}")
                new_plan = query_chatgpt_for_plan_update(goal, "\n".join(initial_plan), response_data['failureReason'],  response_data['inventory'],  response_data['info'])
                print("New plan: " + str(new_plan))
                update_message = {
                    "type": "planUpdate",
                    "body": new_plan
                }
                await websocket.send(json.dumps(update_message))
            elif response_data['type'] == 'setGoal':
                goal = response_data['body'][0]
                print("Received goal " + goal)
                initial_plan = query_chatgpt_for_initial_plan(goal)
                print("Initial plan: " + str(initial_plan))
                initial_message = {
                    "type": "initialPlan",
                    "body": initial_plan
                }
                await websocket.send(json.dumps(initial_message))
            else:
                print(f"Received: {response}")

command_prompt = (
    f"Now, generate a series of commands to accomplish the given goal according to a user-provided plan. "
    f"The available commands are:\n"
    f"1. goto_block block_type - go to a block (e.g., goto_block portal or goto_block ender_chest).\n"
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
    f"Given the goal: \"{{goal}}\", generate a list of commands to achieve it. Output nothing but the commands, separated by newlines."
)

def query_chatgpt_for_initial_plan(goal):
    system_prompt = (
        f"You serve as an assistant that helps me play Minecraft."
    )
    plan_prompt = (
        f"I will give you my goal in the game, please break it down as a tree-structure plan to achieve this goal. The requirements of the tree-structure plan are:\n"
        f"1. The plan tree should be exactly of depth 2.\n"
        f"2. Describe each step in one line.\n"
        f"3. You should index the two levels like ’1.’, ’1.1.’, ’1.2.’, ’2.’, ’2.1.’, etc.\n"
        f"4. The sub-goals at the bottom level should be basic actions so that I can easily execute them in the game."
        f"Given the goal: \"{goal}\", generate a tree-structure plan to achieve it. Output nothing but the plan."
    )
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": plan_prompt}
        ],
        max_tokens=150
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
        max_tokens=150
    )
    return command_response.choices[0].message.content.strip().split('\n')

def query_chatgpt_for_plan_update(goal, plan, failure_reason, inventory, info):
    system_prompt = (
        f"You serve as an assistant that helps me play Minecraft."
    )
    replan_prompt = (
        f"{failure_reason}. Please reason through why this might have happened, and plan a new method of achieving the goal: \"{goal}\", "
        f"starting from the step that failed. Your current inventory is: {inventory}. Additionally, {info}"
    )
    recommand_prompt = (
        f"Now, generate a series of commands to finish accomplishing the given goal according to the new plan. "
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
        max_tokens=150
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
        max_tokens=150
    )
    return command_response.choices[0].message.content.strip().split('\n')

asyncio.run(send_message())
