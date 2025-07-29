import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Tarea implements Serializable {
    //atributos de la clase
    private static final long serialVersionUID = 1L;
    private static int contadorId = 1;
    private int id;
    private String nombre;
    private int duracion; // duración en segundos
    private boolean enProgreso;
    private boolean tareaCompletada;
    private boolean pausada;
    public static List<Tarea> listaTareas = new ArrayList<>();

    public Tarea(String nombre, int duracionSegundos) {
        this.id = contadorId++;
        this.nombre = nombre;
        this.duracion = duracionSegundos;
        this.enProgreso = false;
        this.tareaCompletada = false;
        this.pausada = false;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    public boolean getEnProgreso() {
        return enProgreso;
    }

    public void setEnProgreso(boolean enProgreso) {
        this.enProgreso = enProgreso;
    }

    public boolean getTareaCompletada() {
        return tareaCompletada;
    }

    public void setTareaCompletada(boolean tareaCompletada) {
        this.tareaCompletada = tareaCompletada;
    }

    public boolean isPausada() {
        return pausada;
    }

    public void pausar() {
        this.pausada = true;
    }

    public void continuar() {
        this.pausada = false;
    }

    // Retorna el tiempo restante en formato mm:ss
    public String obtenerTiempoRestanteFormato() {
        int minutos = duracion / 60;
        int segundos = duracion % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    // Metodo para crear una tarea (ejemplo con mensaje opcional)
    public static void crearTarea(Object ventana, Tarea tarea) {
        listaTareas.add(tarea);
        guardarEnArchivoTareas();
    }

    // Guarda la lista de tareas en un archivo (serialización)
    public static void guardarEnArchivoTareas() {
        String dataDir = System.getProperty("user.home") + File.separator + ".kanban-app";
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String filePath = dataDir + File.separator + "tareas.dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(listaTareas);
        } catch (IOException e) {
            System.err.println("Error al guardar las tareas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Carga la lista de tareas desde el archivo
    public static List<Tarea> cargarDatosDesdeArhivo() {
        List<Tarea> tareas = new ArrayList<>();
        String dataDir = System.getProperty("user.home") + File.separator + ".kanban-app";
        String filePath = dataDir + File.separator + "tareas.dat";
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.out.println("Archivo de tareas no encontrado, creando nueva lista.");
            return tareas;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            @SuppressWarnings("unchecked")
            List<Tarea> tareasLeidas = (List<Tarea>) ois.readObject();
            tareas = tareasLeidas;
            // Actualizar contadorId para que no haya IDs repetidos
            int maxId = 0;
            for (Tarea t : tareas) {
                if (t.getId() > maxId) {
                    maxId = t.getId();
                }
            }
            // Ensure contadorId is always at least maxId + 1, but never goes backwards
            contadorId = Math.max(contadorId, maxId + 1);
        } catch (IOException | ClassNotFoundException e) {
            // Si no existe archivo o error, retornamos lista vacía
            System.out.println("No se pudo cargar archivo de tareas, creando nueva lista.");
        }
        return tareas;
    }
}
