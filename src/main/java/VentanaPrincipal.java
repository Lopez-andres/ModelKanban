import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.EventObject;

public class VentanaPrincipal extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable tablaNuevas, tablaProgreso, tablaCompletadas;
    private DefaultTableModel modeloNuevas, modeloProgreso, modeloCompletadas;
    private JLabel temporizadorLabel;
    private Timer timer;
    private boolean enDescanso = false;
    private boolean temporizadorPausado = false;
    private int tiempoRestante = 1500; //25 minutos en segundos
    private List<Tarea> tareasEnProgreso = new ArrayList<>();
    private javax.swing.Timer cronometro;

    public VentanaPrincipal() {
        setTitle("Gestión de Tareas Kanban + Pomodoro");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Tarea.listaTareas = new ArrayList<>(Tarea.cargarDatosDesdeArhivo());

        inicializarComponentes();
        cargarTareasEnTablas();
        configurarBotonesEnTabla();
        iniciarCronometro();
    }

    private void inicializarComponentes() {
        tabbedPane = new JTabbedPane();

        // Panel Tareas Nuevas
        JPanel panelNuevas = new JPanel(new BorderLayout());
        modeloNuevas = new DefaultTableModel(new String[]{"ID", "Nombre", "Duración (min)"}, 0);

        tablaNuevas = new JTable(modeloNuevas);

        //Selección completa por fila
        tablaNuevas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaNuevas.setRowSelectionAllowed(true);
        tablaNuevas.setColumnSelectionAllowed(false);
        tablaNuevas.setCellSelectionEnabled(false);

        // Estética de selección
        tablaNuevas.setSelectionBackground(new Color(84, 132, 185));
        tablaNuevas.setSelectionForeground(Color.BLACK);

        //panel de nuevas tareas
        panelNuevas.add(new JScrollPane(tablaNuevas), BorderLayout.CENTER);

        //panel de creacion de tareas
        JPanel panelCrear = new JPanel();
        JTextField campoNombre = new JTextField(10);
        JTextField campoDuracion = new JTextField(5);

        JButton botonCrear = new JButton("Crear tarea");
        JButton botonMoverAProgreso = new JButton("Mover a En Progreso");

        panelCrear.add(new JLabel("Nombre: "));
        panelCrear.add(campoNombre);
        panelCrear.add(new JLabel("Duración (min): "));
        panelCrear.add(campoDuracion);
        panelCrear.add(botonCrear);
        panelCrear.add(new JLabel("Ejecutar tarea: "));
        JTextField campoEjecutar= new JTextField(3);

        panelCrear.add(campoEjecutar);
        panelCrear.add(botonMoverAProgreso);
        panelNuevas.add(panelCrear, BorderLayout.SOUTH);

        botonCrear.addActionListener(e -> {
            try {
                String nombre = campoNombre.getText().trim();
                int duracionMinutos = Integer.parseInt(campoDuracion.getText().trim());
                if (!nombre.isEmpty() && duracionMinutos > 0) {
                    campoNombre.setText("");
                    campoDuracion.setText("");
                    int duracionSegundos = duracionMinutos * 60;
                    Tarea nuevaTarea = new Tarea(nombre, duracionSegundos);
                    Tarea.crearTarea(this, nuevaTarea);
                    cargarTareasEnTablas();
                } else {
                    JOptionPane.showMessageDialog(this, "Nombre vacío o duración inválida.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Duración inválida, ingresa un número entero.");
            }
        });

        botonMoverAProgreso.addActionListener(e -> {
            try {
                String textoId = campoEjecutar.getText().trim();
                if (textoId.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ingresa el ID de la tarea a ejecutar.");
                    return;
                }

                int id = Integer.parseInt(textoId);
                boolean encontrada = false;

                for (Tarea t : Tarea.listaTareas) {
                    if (t.getId() == id && !t.getTareaCompletada( ) && !t.getEnProgreso()) {
                        t.setEnProgreso(true);
                        t.continuar();
                        encontrada = true;
                        break;
                    }
                }

                if (encontrada) {
                    campoEjecutar.setText("");
                    Tarea.guardarEnArchivoTareas();
                    cargarTareasEnTablas();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró una tarea válida con ese ID.");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID inválido. Debe ser un número entero.");
            }
        });

        // Panel En Progreso
        JPanel panelProgreso = new JPanel(new BorderLayout());
        modeloProgreso = new DefaultTableModel(new String[]{"ID", "Nombre", "Tiempo Restante", "Pausar", "Continuar"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3 || column == 4;
            }
        };
        tablaProgreso = new JTable(modeloProgreso);
        panelProgreso.add(new JScrollPane(tablaProgreso), BorderLayout.CENTER);

        JPanel botonesProgreso = new JPanel();
        botonesProgreso.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Campo para ingresar el ID a completar
        botonesProgreso.add(new JLabel("Tarea completada ID: "));
        JTextField campoCompletar = new JTextField(4);
        botonesProgreso.add(campoCompletar);

        // Botón para completar la tarea
        JButton botonCompletar = new JButton("Marcar como Completada");
        botonesProgreso.add(botonCompletar);

        panelProgreso.add(botonesProgreso, BorderLayout.SOUTH);

        modeloProgreso.addTableModelListener(e -> {
            int fila = e.getFirstRow();
            int columna = e.getColumn();

            // Solo nos interesa la columna de Tiempo Restante (índice 2)
            if (columna == 2 && fila >= 0 && fila < tareasEnProgreso.size()) {
                String valor = modeloProgreso.getValueAt(fila, columna).toString().trim();
                try {
                    if (valor.equals("0") || valor.equals("00:00")) {
                        // Si el tiempo es 0 o 00:00, aceptamos sin error
                        Tarea tarea = tareasEnProgreso.get(fila);
                        tarea.setDuracion(0);
                        Tarea.guardarEnArchivoTareas();
                        return;
                    }

                    String[] partes = valor.split(":");
                    int minutos, segundos;
                    if (partes.length == 2) {
                        minutos = Integer.parseInt(partes[0]);
                        segundos = Integer.parseInt(partes[1]);
                    } else {
                        // Si solo ponen minutos sin ":" lo tratamos como minutos enteros
                        minutos = Integer.parseInt(valor);
                        segundos = 0;
                    }
                    int nuevoTiempo = minutos * 60 + segundos;

                    if (nuevoTiempo < 0) {  // Si es negativo sí error
                        throw new NumberFormatException();
                    }

                    Tarea tarea = tareasEnProgreso.get(fila);
                    tarea.setDuracion(nuevoTiempo);
                    Tarea.guardarEnArchivoTareas();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Formato inválido. Usa MM o MM:SS. Ej: 10 o 10:30");
                    cargarTareasEnTablas(); // Revertir cambio
                }
            }
        });

        botonCompletar.addActionListener(e -> {
            try {
                String textoId = campoCompletar.getText().trim();
                if (textoId.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ingresa el ID de la tarea a completar.");
                    return;
                }

                int id = Integer.parseInt(textoId);
                boolean encontrada = false;

                for (Tarea t : Tarea.listaTareas) {
                    if (t.getId() == id) {
                        if (t.getEnProgreso() && !t.getTareaCompletada()) {
                            t.setEnProgreso(false);
                            t.setTareaCompletada(true);
                            t.pausar();  // Por si estaba corriendo
                            encontrada = true;
                            break;
                        } else {
                            JOptionPane.showMessageDialog(this, "La tarea no está en progreso o ya está completada.");
                            return;
                        }
                    }
                }

                if (encontrada) {
                    campoCompletar.setText("");
                    Tarea.guardarEnArchivoTareas();
                    cargarTareasEnTablas();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró una tarea con ese ID.");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID inválido. Debe ser un número entero.");
            }
        });


        // Panel Completadas
        JPanel panelCompletadas = new JPanel(new BorderLayout());
        modeloCompletadas = new DefaultTableModel(new String[]{"ID", "Nombre"}, 0);
        tablaCompletadas = new JTable(modeloCompletadas);
        panelCompletadas.add(new JScrollPane(tablaCompletadas), BorderLayout.CENTER);

        // Panel inferior con campo y botón
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Etiqueta + campo para ID
        panelInferior.add(new JLabel("Eliminar tarea ID: "));
        JTextField campoEliminar = new JTextField(4);
        panelInferior.add(campoEliminar);

        // Botón de eliminar
        JButton botonEliminar = new JButton("Eliminar tarea");
        panelInferior.add(botonEliminar);

        panelCompletadas.add(panelInferior, BorderLayout.SOUTH);

        botonEliminar.addActionListener(e -> {
            try {
                String textoId = campoEliminar.getText().trim();
                if (textoId.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ingresa el ID de la tarea a eliminar.");
                    return;
                }

                int id = Integer.parseInt(textoId);
                boolean eliminada = false;

                Iterator<Tarea> iterator = Tarea.listaTareas.iterator();
                while (iterator.hasNext()) {
                    Tarea t = iterator.next();
                    if (t.getId() == id) {
                        if (t.getTareaCompletada()) {
                            iterator.remove();
                            eliminada = true;
                            break;
                        } else {
                            JOptionPane.showMessageDialog(this, "La tarea no está marcada como completada.");
                            return;
                        }
                    }
                }

                if (eliminada) {
                    campoEliminar.setText("");
                    Tarea.guardarEnArchivoTareas();
                    cargarTareasEnTablas();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró una tarea con ese ID.");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID inválido. Debe ser un número entero.");
            }
        });


        // Panel Pomodoro
        JPanel panelPomodoro = new JPanel(new BorderLayout());

        // Etiqueta del temporizador
        temporizadorLabel = new JLabel("25:00", SwingConstants.CENTER);
        temporizadorLabel.setFont(new Font("Arial", Font.BOLD, 48));
        panelPomodoro.add(temporizadorLabel, BorderLayout.CENTER);

        // Panel inferior con botones
        JPanel panelBotonesPomodoro = new JPanel(new FlowLayout());
        JButton botonIniciarPomodoro = new JButton("Iniciar Pomodoro");
        JButton botonPausarPomodoro = new JButton("Pausar");
        JButton botonContinuarPomodoro = new JButton("Continuar");

        panelBotonesPomodoro.add(botonIniciarPomodoro);
        panelBotonesPomodoro.add(botonPausarPomodoro);
        panelBotonesPomodoro.add(botonContinuarPomodoro);

        panelPomodoro.add(panelBotonesPomodoro, BorderLayout.SOUTH);

        // Acción del botón Iniciar
        botonIniciarPomodoro.addActionListener(e -> {
            if (temporizadorPausado) {
                // Reanudar desde pausa
                temporizadorPausado = false;
                iniciarTemporizador();
            } else {
                // Iniciar nuevo Pomodoro
                tiempoRestante = 1500; //25 minutos
                enDescanso = false;
                iniciarTemporizador();
            }
        });

        // Acción del botón Pausar
        botonPausarPomodoro.addActionListener(e -> {
            if (timer != null) {
                timer.cancel();
                temporizadorPausado = true;
            }
        });

        // Acción del botón Continuar
        botonContinuarPomodoro.addActionListener(e -> {
            if (temporizadorPausado && tiempoRestante > 0) {
                temporizadorPausado = false;
                iniciarTemporizador();
            }
        });

        //creacion de columnas a las tablas
        tabbedPane.addTab("Tareas Nuevas", panelNuevas);
        tabbedPane.addTab("En Progreso", panelProgreso);
        tabbedPane.addTab("Completadas", panelCompletadas);
        tabbedPane.addTab("Pomodoro", panelPomodoro);

        add(tabbedPane);
    }

    //metodo para iniciar el temporizador del pomodoro
    private void iniciarTemporizador() {
        if (timer != null) timer.cancel();

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (tiempoRestante <= 0) {
                        reproducirSonido("src/sonidos/alarma_wav.wav"); //aquí se llama al sonido

                        timer.cancel();
                        if (enDescanso) {
                            JOptionPane.showMessageDialog(null, "Fin del descanso. Retoma el trabajo.");
                            tiempoRestante = 1500;//25 minutos
                            enDescanso = false;
                        } else {
                            JOptionPane.showMessageDialog(null, "Fin del Pomodoro. ¡Hora de descansar!");
                            tiempoRestante = 600;//10 minutos
                            enDescanso = true;
                        }
                        iniciarTemporizador();
                    } else {
                        int minutos = tiempoRestante / 60;
                        int segundos = tiempoRestante % 60;
                        temporizadorLabel.setText(String.format("%02d:%02d", minutos, segundos));
                        tiempoRestante--;
                    }
                });
            }
        }, 0, 1000);
    }

    private void reproducirSonido(String ruta) {
        try {
            File archivoSonido = new File(ruta);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoSonido);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarTareasEnTablas() {
        modeloNuevas.setRowCount(0);
        modeloProgreso.setRowCount(0);
        modeloCompletadas.setRowCount(0);
        tareasEnProgreso.clear();

        for (Tarea t : Tarea.listaTareas) {
            if (t.getTareaCompletada()) {
                modeloCompletadas.addRow(new Object[]{t.getId(), t.getNombre()});
            } else if (t.getEnProgreso()) {
                tareasEnProgreso.add(t);
                modeloProgreso.addRow(new Object[]{t.getId(), t.getNombre(), t.obtenerTiempoRestanteFormato(), "⏸️", "▶️"});
            } else {
                modeloNuevas.addRow(new Object[]{t.getId(), t.getNombre(), t.getDuracion() / 60});
            }
        }
    }

    private void iniciarCronometro() {
        cronometro = new javax.swing.Timer(1000, e -> {
            for (int i = 0; i < tareasEnProgreso.size(); i++) {
                Tarea t = tareasEnProgreso.get(i);
                if (!t.isPausada()) {
                    int tiempo = t.getDuracion() - 1;
                    t.setDuracion(Math.max(tiempo, 0));
                    if (t.getDuracion() <= 0) {
                        t.setTareaCompletada(true);
                        t.setEnProgreso(false);
                        t.pausar();
                    }
                }
                modeloProgreso.setValueAt(t.obtenerTiempoRestanteFormato(), i, 2);
            }
            Tarea.guardarEnArchivoTareas();
            cargarTareasEnTablas();
        });
        cronometro.start();
    }

    private void configurarBotonesEnTabla() {
        tablaProgreso.getColumn("Pausar").setCellRenderer(new BotonRenderer("⏸️"));
        tablaProgreso.getColumn("Pausar").setCellEditor(new BotonEditor(new JCheckBox(), true));
        tablaProgreso.getColumn("Continuar").setCellRenderer(new BotonRenderer("▶️"));
        tablaProgreso.getColumn("Continuar").setCellEditor(new BotonEditor(new JCheckBox(), false));
    }

    class BotonRenderer extends JButton implements TableCellRenderer {
        public BotonRenderer(String text) {
            setText(text);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    class BotonEditor extends DefaultCellEditor {
        private JButton button;
        private boolean esPausa;
        private int fila;

        public BotonEditor(JCheckBox checkBox, boolean esPausa) {
            super(checkBox);
            this.esPausa = esPausa;
            button = new JButton(esPausa ? "⏸️" : "▶️");

            button.addActionListener(e -> {
                Tarea tarea = tareasEnProgreso.get(fila);
                if (esPausa) tarea.pausar();
                else tarea.continuar();
                fireEditingStopped();
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.fila = row;
            return button;
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
