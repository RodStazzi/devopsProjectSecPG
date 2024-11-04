package cl.rosta.devopsProject.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cl.rosta.devopsProject.configuracion.DatabaseConnectionManager;
import cl.rosta.devopsProject.modelo.Alumno;
import cl.rosta.devopsProject.modelo.AlumnoMap;
import cl.rosta.devopsProject.modelo.Asistencia;
import cl.rosta.devopsProject.modelo.Curso;
import cl.rosta.devopsProject.modelo.Estado;
import cl.rosta.devopsProject.modelo.Nota;
import cl.rosta.devopsProject.modelo.Trabajo;


@Service
public class AlumnoServiceImpl implements AlumnoService {

    private final DatabaseConnectionManager dbManager;

    @Autowired
    public AlumnoServiceImpl(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }
    
    @Autowired
    private PlatformTransactionManager transactionManager;

//	@Autowired
//	private JdbcTemplate jdbcTemplate;

	@Override
	public Alumno getAllAlumnobyId(String rut) {

		String sql = "SELECT " + "    al.id_alumno, " + "    al.nombre, " + "    al.apellido, " + "    al.correo, "
				+ "    al.fecha_nacimiento, al.cuiper, al.rut, " + "    a.id_asistencia, "
				+ "    a.id_alumno_curso AS id_alumno_curso_asistencia, " + "    a.\"presentePrimera\", "
				+ "    a.\"presenteSegunda\", " + "    a.fecha AS fecha_asistencia, " + "    a.pagado, " + "    n.id_nota, "
				+ "    n.id_alumno_curso AS id_alumno_curso_nota, " + "    n.nota,   (select nombre_trabajo from trabajo where id_trabajo = n.id_trabajo) AS nombre_trabajo, "
				+ "    n.fecha AS fecha_nota, eac.id_estado_alumno_curso as idEstado ,"
				+ "    eac.descripcion AS estado  " + " FROM " + "    alumno AS al "
				+ "    LEFT JOIN alumno_curso AS ac ON al.id_alumno = ac.id_alumno "
				+ "    LEFT JOIN nota AS n ON ac.id_alumno_curso = n.id_alumno_curso "
				+ "    LEFT JOIN asistencia AS a ON ac.id_alumno_curso = a.id_alumno_curso "
				+ "    LEFT JOIN estado_alumno_curso AS eac ON ac.id_estado_alumno_curso = eac.id_estado_alumno_curso"
				+ " WHERE " + "    al.rut = ? ";

		AlumnoMap rowMapper = new AlumnoMap();
//		jdbcTemplate.query(sql, new Object[] { rut }, rowMapper);
//	    System.out.println("Alumno "+rowMapper.getAlumnoById(rut));
//		return rowMapper.getAlumnoById(rut);
        dbManager.executeQueryForList(sql, rowMapper, rut);
        System.out.println("Alumno "+rowMapper.getAlumnoById(rut));
        return rowMapper.getAlumnoById(rut);
	}

	@Override
	public boolean insertAlumno(Alumno alumno) {

		String sqlAlumno = "INSERT INTO alumno (nombre, correo, apellido, fecha_nacimiento, cuiper, rut) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_alumno";

		Long idAlumno = dbManager.executeQuery(
				 sqlAlumno, 
					 (rs, rowNum) -> rs.getLong(1),
						 alumno.getNombre(), 
						 alumno.getCorreo(), 
						 alumno.getApellido(), 
						 alumno.getFecha_nacimiento(), 
						 alumno.isCuiper(), 
						 alumno.getRut()
					 );
	    
		
		if (idAlumno != null) {

	        String sqlAlumnoCurso = "INSERT INTO alumno_curso (id_alumno, id_curso, id_estado_alumno_curso) VALUES (?, ?, ?)";
	        return dbManager.executeUpdate(sqlAlumnoCurso, idAlumno, 1, 1);
		}

		return false;
	}

	@Override
	public List<Alumno> getAllAlumnos() {
		String sql = "SELECT al.id_alumno, al.nombre, al.apellido, al.correo, al.fecha_nacimiento, al.cuiper,  al.rut, a.id_asistencia, "
				+ " a.id_alumno_curso AS id_alumno_curso_asistencia, a.\"presentePrimera\", a.\"presenteSegunda\", a.fecha AS fecha_asistencia, "
				+ " a.pagado, n.id_nota, n.id_alumno_curso AS id_alumno_curso_nota, "
				+ " n.nota,   (select nombre_trabajo from trabajo where id_trabajo = n.id_trabajo) AS nombre_trabajo, "
				+ " n.fecha AS fecha_nota, eac.id_estado_alumno_curso as idEstado , eac.descripcion AS estado"
				+ " FROM alumno AS al " + "LEFT JOIN alumno_curso AS ac ON al.id_alumno = ac.id_alumno "
				+ "LEFT JOIN asistencia AS a ON ac.id_alumno_curso = a.id_alumno_curso "
				+ "LEFT JOIN nota AS n ON ac.id_alumno_curso = n.id_alumno_curso "
				+ "LEFT JOIN estado_alumno_curso AS eac ON ac.id_estado_alumno_curso = eac.id_estado_alumno_curso";

		AlumnoMap rowMapper = new AlumnoMap();
        dbManager.executeQueryForList(sql, rowMapper);
        return rowMapper.getAlumnoDetalleList();
	}

	@Override
	public boolean updateAlumno(Alumno alumno) {
		
		    return executeInTransaction(() -> {
		        String sqlAlumno = "UPDATE alumno SET nombre = ?, correo = ?, apellido = ?, fecha_nacimiento = ?, cuiper = ? WHERE id_alumno = ?";
		        boolean resultAlumno = dbManager.executeUpdate(sqlAlumno,
		            alumno.getNombre(),
		            alumno.getCorreo(),
		            alumno.getApellido(),
		            alumno.getFecha_nacimiento(),
		            alumno.isCuiper(),
		            alumno.getId_alumno()
		        );

		        if (!resultAlumno) {
		            return false;
		        }

		        Long idAlumnoCurso = null;
		        try {
		            idAlumnoCurso = dbManager.executeQuery(
		                "SELECT id_alumno_curso FROM alumno_curso WHERE id_alumno = ?",
		                (rs, rowNum) -> rs.getLong(1),
		                alumno.getId_alumno()
		            );
		        } catch (Exception e) {
		            return false;
		        }

		        // Actualizar asistencias
		        if (alumno.getAsistencias() != null && !alumno.getAsistencias().isEmpty()) {
		            for (Asistencia asistencia : alumno.getAsistencias()) {
		                if (asistencia.getId_asistencia() != null) {
		                    // Actualizar asistencia existente
		                    String sqlAsistencia = "UPDATE asistencia SET fecha = ?, presentePrimera = ?, presenteSegunda = ?, pagado = ? " +
		                                         "WHERE id_asistencia = ? AND id_alumno_curso = ?";
		                    dbManager.executeUpdate(sqlAsistencia,
		                        asistencia.getFecha(),
		                        asistencia.isPresentePrimera(),
		                        asistencia.isPresenteSegunda(),
		                        asistencia.isPagado(),
		                        asistencia.getId_asistencia(),
		                        idAlumnoCurso
		                    );
		                } else {
		                    // Insertar nueva asistencia
		                    String sqlAsistencia = "INSERT INTO asistencia (id_alumno_curso, fecha, presentePrimera, presenteSegunda, pagado) " +
		                                         "VALUES (?, ?, ?, ?, ?)";
		                    dbManager.executeUpdate(sqlAsistencia,
		                        idAlumnoCurso,
		                        asistencia.getFecha(),
		                        asistencia.isPresentePrimera(),
		                        asistencia.isPresenteSegunda(),
		                        asistencia.isPagado()
		                    );
		                }
		            }
		        }

		        // Actualizar notas
		        if (alumno.getNotas() != null && !alumno.getNotas().isEmpty()) {
		            for (Nota nota : alumno.getNotas()) {
		                if (nota.getId_nota() != null) {
		                    // Actualizar nota existente
		                    String sqlNota = "UPDATE nota SET nota = ?, fecha = ? WHERE id_nota = ?";
		                    dbManager.executeUpdate(sqlNota,
		                        nota.getNota(),
		                        nota.getFecha(),
		                        nota.getId_nota()
		                    );
		                } else {
		                    // Insertar nueva nota
		                    String sqlNota = "INSERT INTO nota (id_alumno_curso, nota, fecha) VALUES (?, ?, ?)";
		                    dbManager.executeUpdate(sqlNota,
		                        idAlumnoCurso,
		                        nota.getNota(),
		                        nota.getFecha()
		                    );
		                }
		            }
		        }

		        // Actualizar estado del alumno curso si es necesario
		        if (alumno.getIdEstadoAlumnoCurso() != null) {
		            String sqlEstado = "UPDATE alumno_curso SET id_estado_alumno_curso = ? WHERE id_alumno_curso = ?";
		            dbManager.executeUpdate(sqlEstado,
		                alumno.getIdEstadoAlumnoCurso(),
		                idAlumnoCurso
		            );
		        }

		        return true;
		    });

	}

	@Override
	public List<Alumno> getAllAlumnosFront() {

		String sql = "SELECT al.id_alumno, al.nombre, al.apellido, al.correo, al.fecha_nacimiento, al.cuiper, a.id_asistencia, a.id_alumno_curso AS id_alumno_curso_asistencia, a.\"presentePrimera\", a.\"presenteSegunda\", a.fecha AS fecha_asistencia, a.pagado, n.id_nota, n.id_alumno_curso AS id_alumno_curso_nota, n.nota, n.fecha AS fecha_nota, eac.id_estado_alumno_curso as idEstado , eac.descripcion AS estado"
				+ " FROM alumno AS al " + "LEFT JOIN alumno_curso AS ac ON al.id_alumno = ac.id_alumno "
				+ "LEFT JOIN asistencia AS a ON ac.id_alumno_curso = a.id_alumno_curso "
				+ "LEFT JOIN nota AS n ON ac.id_alumno_curso = n.id_alumno_curso "
				+ "LEFT JOIN estado_alumno_curso AS eac ON ac.id_estado_alumno_curso = eac.id_estado_alumno_curso "
				+ " where eac.descripcion in ('Inscrito', 'En Curso', 'Suspendido')";

		AlumnoMap rowMapper = new AlumnoMap();
        dbManager.executeQueryForList(sql, rowMapper);
        return rowMapper.getAlumnoDetalleList();
		
	}

	@Override
	public boolean deleteNota(Long id) {
		String sql = "delete from nota where id_nota =  ?";
		return dbManager.executeUpdate(sql, id);
	}

	@Override
	public boolean deleteAsistencia(Long id) {
		String sql = "delete from asistencia where id_asistencia  = ?";
		return dbManager.executeUpdate(sql, id);
		
	}

	@Override
	public boolean deleteAlumno(Long id) {
		int result = 0;
		try {
			String sql = "UPDATE alumno_curso SET id_estado_alumno_curso = 5 WHERE id_alumno = ?";
			return dbManager.executeUpdate(sql, id);
		} catch (Exception e) {
			
		}
//		if (result > 0)
//			return true;
//		return false;
		return false;
	}

	@Override
	public boolean insertAsistencia(Long idAlumnoCurso, LocalDate fecha, boolean presentePrimera,
			boolean presenteSegunda, boolean pagado) {
	      String sqlAsistencia = "INSERT INTO asistencia (id_alumno_curso, fecha, \"presentePrimera\", \"presenteSegunda\", pagado) VALUES (?, ?, ?, ?, ?)";
			return dbManager.executeUpdate(sqlAsistencia, idAlumnoCurso, fecha, presentePrimera, presenteSegunda, pagado);
			
	}

	@Override
	public boolean modificAsistencia(Long idAlumnoCurso, Long idAsistencia, LocalDate fecha, boolean presentePrimera,
			boolean presenteSegunda, boolean pagado) {
        String sqlAsistencia = "UPDATE asistencia SET fecha = ?, \"presentePrimera\" = ?, \"presenteSegunda\" = ?, pagado = ? WHERE id_asistencia = ? AND id_alumno_curso = ? ";
        return dbManager.executeUpdate(sqlAsistencia,
                fecha,
                presentePrimera,
                presenteSegunda,
                pagado,
                idAsistencia,
                idAlumnoCurso
            );
        
	}
	
	@Override
	public boolean modificNota(float nota, LocalDate fecha, Long idNota) {
        String sqlNota = "UPDATE nota SET nota = ?, fecha = ? WHERE id_nota = ?";
        return dbManager.executeUpdate(sqlNota, nota, fecha, idNota);

	}

	@Override
	public boolean insertNota(Long idAlumnoCurso, float nota, LocalDate fecha, Long id_trabajo) {
        String sqlNota = "INSERT INTO nota (id_alumno_curso, nota, fecha, id_trabajo) VALUES (?, ?, ?, ?)";
        return dbManager.executeUpdate(sqlNota, idAlumnoCurso, nota, fecha, id_trabajo);

	}

	@Override
	public boolean insertAlumnoCurso(Long id_alumno, Long id_curso, Long id_estado_alumno_curso) {
        String sqlAlumnoCurso = "INSERT INTO alumno_curso ( id_alumno, id_curso, id_estado_alumno_curso) VALUES (?, ?, ?)";
        return dbManager.executeUpdate(sqlAlumnoCurso, id_alumno, id_curso, id_estado_alumno_curso);

	}

	@Override
	public boolean modificAlumno( String nombre, String correo, String apellido, LocalDate fecha_nacimiento, boolean cuiper, String rut, Long id_alumno) {

        String sqlAlumno = "UPDATE alumno SET nombre = ?, correo = ?, apellido = ?, fecha_nacimiento = ?, cuiper = ?, rut = ? WHERE id_alumno = ?  ";
        return dbManager.executeUpdate(sqlAlumno, nombre, correo, apellido, fecha_nacimiento, cuiper, rut, id_alumno);
        
	}

	@Override
	public boolean modificAlumnoCurso(Long id_alumno, Long id_curso, Long id_estado_alumno_curso,
			Long id_alumno_curso) {
        String sqlAlumno = "UPDATE alumno_curso SET id_alumno = ?, id_curso = ?, id_estado_alumno_curso = ? WHERE id_alumno_curso = ?";
        return dbManager.executeUpdate(sqlAlumno, id_alumno, id_curso, id_estado_alumno_curso, id_alumno_curso);

	}

	@Override
	public List<Curso> getAllCursos() {
		String sql = "SELECT id_curso, nombre_curso FROM curso "
				+ "WHERE id_curso IN ( "
				+ "    SELECT MIN(id_curso) "
				+ "    FROM curso "
				+ "    GROUP BY nombre_curso "
				+ ")";

        return dbManager.executeQueryForList(sql, (rs, rowNum) -> {
            Curso curso = new Curso();
            curso.setId_curso(rs.getLong("id_curso"));
            curso.setNombre_curso(rs.getString("nombre_curso"));
            return curso;
        });
	}

	@Override
	public List<Estado> getAllEstados() {
		String sql = "SELECT id_estado_alumno_curso, descripcion "
				+ "FROM estado_alumno_curso "
				+ "WHERE id_estado_alumno_curso IN ( "
				+ "    SELECT MIN(id_estado_alumno_curso) "
				+ "    FROM estado_alumno_curso "
				+ "    GROUP BY descripcion "
				+ ")";

        return dbManager.executeQueryForList(sql, (rs, rowNum) -> {
        	Estado estado = new Estado();
        	estado.setId_estado_alumno_curso(rs.getLong("id_estado_alumno_curso"));
        	estado.setDescripcion(rs.getString("descripcion"));
            return estado;
        });
	    
	}

	@Override
	public List<Trabajo> getAllTrabajos() {
		String sql = "SELECT id_trabajo, nombre_trabajo FROM trabajo ";

        return dbManager.executeQueryForList(sql, (rs, rowNum) -> {
        	Trabajo t = new Trabajo();
        	t.setId_trabajo(rs.getLong("id_trabajo"));
        	t.setNombre_trabajo(rs.getString("nombre_trabajo"));
            return t;
        });
	    
	}
	
	@Override
	public boolean insertarAlumno(String nombre, String correo, String apellido, LocalDate fecha_nacimiento, boolean cuiper, String rut) {

		String sqlAlumno = "INSERT INTO alumno (nombre, correo, apellido, fecha_nacimiento, cuiper, rut) VALUES (?, ?, ?, ?, ?, ?)";
        return dbManager.executeUpdate(sqlAlumno, nombre, correo, apellido,fecha_nacimiento,cuiper, rut);

	}

	@Override
	public Long getId_alumno_curso(Long id) {
	    String sql = "SELECT id_alumno_curso FROM alumno_curso WHERE id_alumno = ?";
	    return dbManager.executeQuery(sql, (rs, rowNum) -> rs.getLong(1), id);

	}
	
    private boolean executeInTransaction(Supplier<Boolean> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> {
            try {
                return operation.get();
            } catch (Exception e) {
                status.setRollbackOnly();
                return false;
            }
        });
    }
}