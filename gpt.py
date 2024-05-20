import openai

api_key = ''
openai.api_key = api_key

def chat_with_gpt3(prompt):
    try:
        # Make an API call to ChatGPT
        response = openai.Completion.create(
            engine="gpt-4o",  # Use 'text-davinci-002' for ChatGPT (GPT-3.5)
            prompt=prompt,
            max_tokens=150,  # You can adjust this value to control response length
        )
        return response.choices[0].text.strip()
    except Exception as e:
        return str(e)

# Example usage
if __name__ == "__main__":
    user_input = input("You: ")
    print(user_input)
    init_prompt = ""
    while user_input.lower() not in ['bye', 'quit', 'exit']:
        response = chat_with_gpt3(init_prompt + user_input + "\n")
        print("ChatGPT:", response)
        user_input = input("You: ")
