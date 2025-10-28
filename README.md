# Chatbot Mobile

<p align="center">
  <img src="akasha.svg" alt="Chatbot Logo" width="200"/>
</p>

An Android chatbot application built with Jetpack Compose and Kotlin, featuring a modern UI with markdown support for rich text conversations.

## Features

- 🤖 **AI-Powered Chat**: Engage in conversations with an AI chatbot
- 💬 **Rich Text Support**: Full markdown rendering for formatted messages including:
  - Bold, italic, and inline code
  - Code blocks with syntax highlighting
  - Lists and blockquotes
  - Links
- 🎨 **Modern UI**: Material Design 3 with custom theming
- ☁️ **Cloud Integration**: Powered by Supabase backend
- 📱 **Responsive Design**: Optimized for mobile devices

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Backend**: Supabase
- **Markdown Rendering**: compose-markdowntext
- **Build System**: Gradle (Kotlin DSL)

## Project Structure

```
app/
├── src/
│   └── main/
│       └── java/com/rosuelo/chatbot/
│           ├── ChatBox.kt          # Main chat UI component
│           ├── ChatBotProvider.kt  # Chat logic and API calls
│           ├── SupabaseProvider.kt # Supabase integration
│           └── ui/theme/           # App theming
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 24 or higher
- Kotlin 1.9+

### Installation

1. Clone the repository:

```bash
git clone https://github.com/AetherKnowledge/Chatbot-Mobile.git
```

2. Open the project in Android Studio

3. Configure your Supabase credentials in the appropriate configuration file

4. Build and run the app

## Usage

- Type your message in the text field at the bottom
- Press the send button to send your message
- View AI responses with full markdown formatting
- Messages are saved and synced with your Supabase backend

## UI Components

### ChatBox

The main chat interface featuring:

- Scrollable message list (reversed layout for chat experience)
- User messages aligned right with primary color
- AI messages aligned center with full-width formatting
- Loading indicator during AI response generation

### MessageBubble

Individual message component with:

- Markdown text rendering
- Conditional styling based on message type
- Rounded corners with different shapes for user/AI messages

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

Project Link: [https://github.com/AetherKnowledge/Chatbot-Mobile](https://github.com/AetherKnowledge/Chatbot-Mobile)

---

Made with ❤️ using Jetpack Compose
