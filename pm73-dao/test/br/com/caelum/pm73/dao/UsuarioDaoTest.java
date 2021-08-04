package br.com.caelum.pm73.dao;

import br.com.caelum.pm73.dominio.Usuario;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UsuarioDaoTest {

    private UsuarioDao usuarioDao;
    private Session session;

    @Before
    public void before() {
        this.session = new CriadorDeSessao().getSession();
        this.usuarioDao = new UsuarioDao(this.session);
       this.session.getTransaction().begin();
    }

    @After
    public void after() {
        this.session.getTransaction().rollback();
        this.session.close();
    }

    @Test
    public void deveEncontrarUsuarioPorNomeEEmail() {
        Usuario usuario = new Usuario("Usuario 1", "usuario1@email.com");
        this.usuarioDao.salvar(usuario);

        Usuario encontrado = this.usuarioDao.porNomeEEmail(usuario.getNome(), usuario.getEmail());

        assertEquals("Usuario 1", encontrado.getNome());
        assertEquals("usuario1@email.com", encontrado.getEmail());

    }

    @Test
    public void quandoNomeEEmailNaoExistirMetodoPorNomeEmailRetornaNull() {

        Usuario usuarioNaoEncontrado = usuarioDao.porNomeEEmail("usuario 1", "usuario1@email.com");

        assertNull(usuarioNaoEncontrado);

    }

    @Test
    public void deveDeletarUmUsuario() {
        var usuario = new Usuario("usuario", "usuario@email.com");

        this.usuarioDao.salvar(usuario);
        this.usuarioDao.deletar(usuario);

        this.session.flush();
        this.session.clear();

        var u = this.usuarioDao.porNomeEEmail("usuario", "usuario@email.com");

        assertNull(u);
    }

    @Test
    public void deveAlterarUmUsuario() {
        var usuario = new Usuario("usuario", "usuario@email.com");
        this.usuarioDao.salvar(usuario);

        usuario.setNome("Novo nome");

        this.session.flush();
        this.session.clear();

        Usuario u = this.usuarioDao.porNomeEEmail("usuario", "usuario@email.com");

        assertNull(u);

        u = this.usuarioDao.porNomeEEmail("Novo nome", "usuario@email.com");

        assertEquals("Novo nome", u.getNome());

    }
}
