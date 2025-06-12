# Kanban Pomodoro - Java Desktop Task Management Application

## 🎯 Objective

The goal of this project is to design and develop a desktop application in **Java** that simulates a **Kanban-style task management system**, integrated with a **Pomodoro technique timer**. The system helps users organize tasks across different stages and manage their work/rest cycles efficiently.

---

## 🧩 Functional Requirements

The application includes a graphical user interface built with **Java Swing**, structured into four main tabs:

1. **New Tasks**
   - Allows the user to create new tasks by entering a name and estimated duration (in minutes).
   - Tasks are displayed in a table and can be moved to the next stage.

2. **In Progress**
   - Displays tasks currently being worked on.
   - Users can manually update the remaining time.
   - Tasks automatically move to the "Completed" tab when the time reaches zero.

3. **Completed**
   - Lists all finished tasks.
   - Allows users to delete reviewed tasks.

4. **Pomodoro**
   - A built-in timer that notifies the user after 25 minutes of focused work to take a 10-minute break.
   - After the break, a notification prompts the user to resume work.

---

## 💾 Data Persistence

- Tasks are persistently saved using `.ser` or `.bin` files via **ObjectOutputStream** and **ObjectInputStream**.
- This ensures the application state is preserved across sessions.

---

## 🛠️ Technical Requirements

- Language: **Java**
- GUI: **Swing**
- Serialization: `Tarea` class implements `Serializable`
- Data Structures: `ArrayList`
- Timers: `java.util.Timer`
- Event Handling via Listeners

---

## 📦 Project Structure
KanbanPomodoro/
├── src/
│ ├── Tarea.java
│ ├── VentanaPrincipal.java
├── tareas.bin
├── README.md
└── alarm.wav

## ✅ Evaluation Criteria

- ✔️ Fulfillment of all functional and technical requirements  
- ✔️ Well-structured, clean, and commented code  
- ✔️ Correct implementation of data persistence  
- ✔️ Clear and complete project presentation  

---

## 👨‍💻 Authors

- Andres ([github.com/andresdev](https://github.com/andresdev))  
- [Other team members if any]

---

## 📜 License

This project was developed for academic purposes. You are welcome to use it as a reference or educational material.
