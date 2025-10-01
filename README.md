# Selenium MCP with Spring AI 🤖🧪

A modular command-based Model Context Protocol (MCP) server implementation for **Selenium WebDriver**, using **Spring AI** for intelligent browser automation capabilities. This project supports browser-level control via a standardized set of commands, making it ideal for dynamic testing or AI-assisted browser automation.

## 🛠 Pre-requisites

- **Java 17+**
- **Maven**

## Supported browsers
- **Chrome**
- **Firefox**
- **Safari**

#### Note: For Safari browser, enable automation capabilities.
- Go to Safari > Preferences/Settings > Advanced > check `Show Develop menu in menu bar`
- Now go to the Develop menu in the top menu bar > Allow Remote Automation

## 🚀 Features

This tool includes a wide range of browser control commands:

- ✅ `browser_open`: Launch the browser with desired configuration.
- ✅ `browser_close`: Gracefully shuts down the browser session.
- ✅ `browser_navigate`: Navigate to a specific URL.
- ✅ `browser_click`: Click on web elements.
- ✅ `browser_send_keys`: Type into input fields.
- ✅ `browser_take_screenshot`: Capture full-page screenshots.
- ✅ `browser_get_text`: Retrieve text content from elements.
- ✅ `browser_hover`: Hover over elements.
- ✅ `browser_drag_and_drop`: Perform drag-and-drop actions.
- ✅ `browser_double_click`: Double-click on elements.
- ✅ `browser_right_click`: Right-click/context click on elements.
- ✅ `browser_press_key`: Simulate keyboard key press.
- ✅ `browser_upload_file`: Upload files using file input elements.
- ✅ `browser_page_source`: Extract full HTML page source.
- ✅ `browser_execute_javascript`: Run custom JavaScript in the browser context.
- ✅ `browser_tabs`: Manage and switch between multiple browser tabs.

## 📦 Installation
- Download the latest build (.jar) from [Releases](https://github.com/nipunsaini/selenium-spring-mcp/releases) section
- Add the following config to the `mcp.json` config file of MCP client (Github copilot, Claude desktop etc.)
```json
{
  "mcpServers": {
    "selenium-mcp": {
      "command": "java",
      "args": ["-jar", "path/to/jar/file"]
    }
  }
}
```


## 📦 Build Project

- **Run the following commands in terminal**

```bash
# Clone the repo
git clone https://github.com/nipunsaini/selenium-spring-mcp.git

# move to the project directory
cd selenium-spring-mcp

# build
mvn clean package
```

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change or add.

## 📄 License

This project is licensed under the MIT License.

Built with 💡 by [[Nipun](https://github.com/nipunsaini)]

