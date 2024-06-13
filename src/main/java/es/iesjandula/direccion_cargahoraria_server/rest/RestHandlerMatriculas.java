package es.iesjandula.direccion_cargahoraria_server.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.iesjandula.direccion_cargahoraria_server.exception.HorarioException;
import es.iesjandula.direccion_cargahoraria_server.models.Asignatura;
import es.iesjandula.direccion_cargahoraria_server.models.Curso;
import es.iesjandula.direccion_cargahoraria_server.utils.Parse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
/**
 * clase RestHandlerMatriculas
 */
@RequestMapping(value = "/matriculas")
@RestController
@Log4j2
public class RestHandlerMatriculas
{
	/**
	 * metodo para cargar los cursos
	 * @param csvFile
	 * @param curso
	 * @param etapa
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST, value = "/cursos", consumes = "multipart/form-data")
	public ResponseEntity<?> uploadCursos(@RequestParam(value = "csv", required = true) MultipartFile csvFile,
			@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			Map<String, Map<String, List<String>>> mapaCursos = (Map<String, Map<String, List<String>>>) session
					.getAttribute("mapaCursos");
			mapaCursos = parse.inicializarMapaCursos(session);
			Map<String, List<String>> mapaAsignaturas = new HashMap<String, List<String>>();
			String clave = parse.parseCursosMap(csvFile, curso, etapa, mapaAsignaturas, session);
			mapaAsignaturas = (Map<String, List<String>>) session.getAttribute("mapaAsignaturasCursos");
			mapaCursos.put(clave, mapaAsignaturas);
			session.setAttribute("mapaCursos", mapaCursos);
			log.info(mapaCursos);
			return ResponseEntity.ok().body("Carga realizada con exito");
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}

	/**
	 * endpoint para obtener cursos
	 * 
	 * @param session
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/cursos")
	public ResponseEntity<?> getCursos(HttpSession session) 
	{
		try
		{
			Parse parse = new Parse();
			Map<String, Map<String, List<String>>> mapaCursos = new HashMap<String, Map<String, List<String>>>();
			mapaCursos = parse.comprobarMapaCursosExiste(session, mapaCursos);
			return ResponseEntity.ok().body(mapaCursos.keySet());
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}

	/**
	 * endpoint para subir bloques
	 * 
	 * @param curso
	 * @param etapa
	 * @param nombreAsignatura
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST, value = "/bloques")
	public ResponseEntity<?> uploadBloques(@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa,
			@RequestHeader(value = "nombreAsignatura", required = true) String nombreAsignatura, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			Map<String, List<String>> mapaBloques = (Map<String, List<String>>) session.getAttribute("mapaBloques");
			if (mapaBloques == null)
			{
				mapaBloques = new HashMap<>();
			}
			List<Asignatura> listaAsignaturas = (List<Asignatura>) session.getAttribute("listaAsignaturas");
			parse.comprobarListaAsignaturas(session, listaAsignaturas);

			boolean encontrado = false;
			String resultado = "alguno de los parametros es incorrecto";
			int i = 0;
			// comprobamos que los parametros recibidos son correctos
			while (i < listaAsignaturas.size() && !encontrado)
			{
				if (listaAsignaturas.get(i).getNombreAsinatura().equalsIgnoreCase(nombreAsignatura)
						&& listaAsignaturas.get(i).getCurso() == (curso)
						&& listaAsignaturas.get(i).getEtapa().equalsIgnoreCase(etapa))
				{
					encontrado = true;
				}
				i++;
			}
			if (encontrado)
			{
				String clave = curso + etapa;
				// obtenemos la lista en caso de que la clave exista u obtenemos un nueva lista
				// de asignaturas
				List<String> listaNombreAsignatura = mapaBloques.getOrDefault(clave, new ArrayList<String>());

				if (listaNombreAsignatura.contains(nombreAsignatura))
				{
					resultado = "esa asignatura ya esta registrada";
				}
				else
				{
					listaNombreAsignatura.add(nombreAsignatura);
					mapaBloques.put(clave, listaNombreAsignatura);
					session.setAttribute("mapaBloques", mapaBloques);
					resultado = "Se ha realizado correctamente";
				}
			}
			else
			{
				String error = "alguno de los parametros mandados no existe";
				return ResponseEntity.status(410).body(error);
			}
			return ResponseEntity.ok().body(resultado);
		}
		catch (HorarioException horarioException)
		{
			String error = "Error de parseo";
			log.error(error, horarioException.getBodyExceptionMessage());
			return ResponseEntity.status(410).body(error);
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(error);
		}
	}

	/**
	 * endpoint para obtener los bloques
	 * 
	 * @param curso
	 * @param etapa
	 * @param session
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/bloques")
	public ResponseEntity<?> getBloques(@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			Map<String, List<String>> mapaBloques = new HashMap<String, List<String>>();
			mapaBloques = parse.comprobarMapaBloquesExiste(session, mapaBloques);
			String clave = curso + etapa;
			List<String> listaAsignatura = mapaBloques.get(clave);
			return ResponseEntity.ok().body(listaAsignatura);
		}
		catch (HorarioException horarioException)
		{
			String error = "Error de parseo";
			log.error(error, horarioException.getBodyExceptionMessage());
			return ResponseEntity.status(410).body(error);
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(error);
		}
	}

	/**
	 * metodo para cargar alumnos
	 * 
	 * @param alumno
	 * @param curso
	 * @param etapa
	 * @param grupo
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST, value = "/alumnos")
	public ResponseEntity<?> uploadAlumno(@RequestHeader(value = "alumno", required = true) String alumno,
			@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa,
			@RequestHeader(value = "grupo", required = true) String grupo, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			Curso cursoObject = new Curso(curso, etapa, grupo);
			String resultado = "";
			String clave = curso + etapa + grupo;
			List<Curso> listaCursos = (List<Curso>) session.getAttribute("listaCursos");
			parse.comprobarListaCursos(session, listaCursos);
			List<String> listaNombres = (List<String>) session.getAttribute("listaNombres");
			parse.comprobarListaNombresExiste(session, listaNombres);
			resultado = parse.realizarAsignacionAlumno(alumno, session, cursoObject, resultado, clave, listaCursos,
					listaNombres);

			return ResponseEntity.ok().body(resultado);
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}
	
	/**
	 * metodo para obtener los alumnos
	 * @param curso
	 * @param etapa
	 * @param grupo
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.GET, value = "/alumnos")
	public ResponseEntity<?> getAlumno(@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa,
			@RequestHeader(value = "grupo", required = true) String grupo, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			List<String> listaAlumnos = null;
			List<Curso> listaCursos = (List<Curso>) session.getAttribute("listaCursos");
			parse.comprobarListaCursos(session, listaCursos);
			Curso cursoObject = new Curso(curso, etapa, grupo);
			if (listaCursos.contains(cursoObject))
			{
				String clave = curso + etapa + grupo;
				Map<String, List<String>> mapaAlumnos;
				mapaAlumnos = (Map<String, List<String>>) session.getAttribute("mapaAlumnos");
				if (session.getAttribute("mapaAlumnos") != null)
				{
					mapaAlumnos = (Map<String, List<String>>) session.getAttribute("mapaAlumnos");
				}
				else
				{
					String error = "Los alumnos no han sido cargados en sesion todavía";
					throw new HorarioException(1, error);
				}
				listaAlumnos = mapaAlumnos.get(clave);
			}
			else
			{
				String error = "El curso no existe";
				throw new HorarioException(1, error);
			}
			return ResponseEntity.ok().body(listaAlumnos);

		}
		catch (HorarioException horarioException)
		{
			return ResponseEntity.status(410).body(horarioException.getBodyExceptionMessage());
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}
	
	/**
	 * endpoint para aliminar alumnos
	 * @param alumno
	 * @param curso
	 * @param etapa
	 * @param grupo
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.DELETE, value = "/alumnos")
	public ResponseEntity<?> borrarAlumno(@RequestHeader(value = "alumno", required = true) String alumno,
			@RequestHeader(value = "curso", required = true) Integer curso,
			@RequestHeader(value = "etapa", required = true) String etapa,
			@RequestHeader(value = "grupo", required = true) String grupo, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			String resultado = "No existe el alumno";
			List<Curso> listaCursos = (List<Curso>) session.getAttribute("listaCursos");
			parse.comprobarListaCursos(session, listaCursos);
			Curso cursoObject = new Curso(curso, etapa, grupo);
			if (listaCursos.contains(cursoObject))
			{
				String clave = curso + etapa + grupo;
				Map<String, List<String>> mapaAlumnos;
				mapaAlumnos = (Map<String, List<String>>) session.getAttribute("mapaAlumnos");
				if (session.getAttribute("mapaAlumnos") != null)
				{
					mapaAlumnos = (Map<String, List<String>>) session.getAttribute("mapaAlumnos");
				}
				else
				{
					String error = "Los alumnos no han sido cargados en sesion todavía";
					throw new HorarioException(1, error);
				}
				List<String> listaAlumnos = mapaAlumnos.get(clave);
				log.info(listaAlumnos);
				boolean encontrado = false;
				int i = 0;
				while (i < listaAlumnos.size() && !encontrado)
				{
					if (listaAlumnos.get(i).contains(alumno))
					{
						listaAlumnos.remove(i);
						encontrado = true;
						resultado = "Se ha eliminado correctamente";
					}
					i++;
				}
			}
			else
			{
				String error = "El curso no existe";
				throw new HorarioException(1, error);
			}

			return ResponseEntity.ok().body(resultado);
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}

	/**
	 * 
	 * @param nombreAsignatura
	 * @param curso
	 * @param etapa
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.GET, value = "/asignaturas/resumen")
	public ResponseEntity<?> getAsignaturasResumen(
			@RequestHeader(value = "nombreAsignatura", required = true) String nombreAsignatura, HttpSession session)
	{
		try
		{
			Parse parse = new Parse();
			List<String> listaNombres = (List<String>) session.getAttribute("listaNombres");
			parse.comprobarListaNombresExiste(session, listaNombres);
			Map<String, List<String>> mapaAsignaturas = (Map<String, List<String>>) session.getAttribute("mapaAsignaturasCursos");
			parse.comprobarMapaAsignaturasCursosExiste(session,mapaAsignaturas);
			List<Asignatura> listaAsignatura = (List<Asignatura>) session.getAttribute("listaAsignaturas");
			parse.comprobarListaAsignaturas(session, listaAsignatura);
			int contadorAlumno=0;
			boolean encontrado = false;
			int i = 0;
			while(i < listaAsignatura.size() && !encontrado) 
			{
				if(listaAsignatura.get(i).getNombreAsinatura().equalsIgnoreCase(nombreAsignatura)) 
				{
					encontrado = true;
				}
				i++;
			}
			if(encontrado) 
			{
				for(String nombre : listaNombres) 
				{
					List<String> asignaturasAlumno = mapaAsignaturas.get(nombre);
					if(asignaturasAlumno.contains(nombreAsignatura)) 
					{
						contadorAlumno ++;
					}
				}
			}
			else 
			{
				String error = "La asignatura no existe";
				throw new HorarioException(1, error);
			}
			
			return ResponseEntity.ok().body(contadorAlumno);
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}
	
	/**
	 * endpoint para obtener el resumen de un curso
	 * @param nombreAsignatura
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.GET, value = "/cursos/resumen")
	public ResponseEntity<?> getCursosResumen(
			@RequestHeader(value = "curso", required = true) String curso,
			@RequestHeader(value = "etapa", required = true) String etapa,HttpSession session)
	{
		try
		{
			Map<String, List<String>> mapaAlumnos;
			mapaAlumnos = (Map<String, List<String>>) session.getAttribute("mapaAlumnos");
			
			return ResponseEntity.ok().body("prueba");
		}
		catch (Exception exception)
		{
			String error = "Error desconocido";
			log.error(error, exception.getMessage());
			return ResponseEntity.status(400).body(exception.getMessage());
		}
	}
}