package ar.edu.utn.donatrack.DonantesYContacto;

public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;
  private int edad;
  private String genero;
  private String direccion;

  public PersonaHumana(MedioDeContacto medioDeContacto, String nombre, String apellido, int edad, long numDocumento, TipoDocumento documento, String genero, String direccion) {
    super(validateMedioDeContacto(medioDeContacto), numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }

  private static MedioDeContacto validateMedioDeContacto(MedioDeContacto medioDeContacto) { 
    if (!(medioDeContacto instanceof CorreoElectronico)) {
      throw new IllegalArgumentException("El medio de contacto obligatorio para una persona humana debe ser un Correo Electrónico.");
    }
    return medioDeContacto;
  }

  public void actualizarDatosDesdeCSV(PersonaHumana nuevosDatos) {
    this.nombre = nuevosDatos.getNombre();
    this.apellido = nuevosDatos.getApellido();
    this.numDocumento = nuevosDatos.getNumDocumento();
    this.documento = nuevosDatos.getDocumento();
  }

  public String getNombre() { return nombre; }
  public String getApellido() { return apellido; }
  public int getEdad() { return edad; }
}