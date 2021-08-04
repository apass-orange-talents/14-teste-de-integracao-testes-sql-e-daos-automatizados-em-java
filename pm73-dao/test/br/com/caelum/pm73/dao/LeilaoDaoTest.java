package br.com.caelum.pm73.dao;

import br.com.caelum.pm73.builder.LeilaoBuilder;
import br.com.caelum.pm73.dominio.Lance;
import br.com.caelum.pm73.dominio.Leilao;
import br.com.caelum.pm73.dominio.Usuario;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class LeilaoDaoTest {
    private LeilaoDao leilaoDao;
    private Session session;
    private UsuarioDao usuarioDao;

    @Before
    public void before() {
        this.session = new CriadorDeSessao().getSession();

        this.leilaoDao = new LeilaoDao(this.session);
        this.usuarioDao = new UsuarioDao(this.session);

        this.session.getTransaction().begin();
    }

    @After
    public void after() {
        this.session.getTransaction().rollback();
        this.session.close();
    }


    @Test
    public void deveContarLeiloesNaoEncerrados() {
        Usuario usuario = new Usuario("Usuario 1", "usuario1@email.com");

        Leilao leilaoAtivo = new LeilaoBuilder()
                .comNome("Novo leilão qualquer!")
                .comDono(usuario)
                .constroi();

        Leilao leilaoEncerrado = new LeilaoBuilder()
                .comNome("Leilão Encerrado")
                .comDono(usuario)
                .encerrado()
                .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilaoAtivo);
        this.leilaoDao.salvar(leilaoEncerrado);

        assertEquals(1L, (long) this.leilaoDao.total());
    }

    @Test
    public void quandoNaoExistirLeiloesEncerradosEntaoTotalRetornaZero() {
        Usuario usuario = new Usuario("Usuario 1", "usuario1@email.com");

        Leilao leilaoEncerrado1 = new LeilaoBuilder()
                .comNome("Leilão Encerrado 1")
                .comDono(usuario)
                .encerrado()
                .constroi();

        Leilao leilaoEncerrado2 = new LeilaoBuilder()
                .comNome("Leilão Encerrado 2")
                .comDono(usuario)
                .encerrado()
                .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilaoEncerrado1);
        this.leilaoDao.salvar(leilaoEncerrado2);

        assertEquals(0L, (long) this.leilaoDao.total());
    }

    @Test
    public void verificaSeOMetodoNovosRetornaApenasOsLeiloesDeProdutosNovos() {
        Usuario usuario = new Usuario("Usuario 1", "usuario1@email.com");

        Leilao leilaoUsado = new LeilaoBuilder()
                .comNome("Leilão Encerrado 1")
                .comDono(usuario)
                .usado()
                .constroi();

        Leilao leilaoNovo = new LeilaoBuilder()
                .comNome("Leilão Encerrado 2")
                .comDono(usuario)
                .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilaoUsado);
        this.leilaoDao.salvar(leilaoNovo);

        List<Leilao> leiloesDeNovos = this.leilaoDao.novos();

        assertEquals(1L, leiloesDeNovos.size());
        assertEquals(false, leiloesDeNovos.get(0).isUsado());
    }

    @Test
    public void testaSeAntigosRetornaSomenteLeiloesAnterioesA7Dias() {
        Usuario usuario = new Usuario("Usuario 1", "usuario1@email.com");

        Leilao leilao2Dias = new LeilaoBuilder()
                .comNome("Leilão Encerrado 1")
                .comDono(usuario)
                .diasAtras(2)
                .constroi();

        Leilao leilao8Dias = new LeilaoBuilder()
                .comNome("Leilão Encerrado 2")
                .comDono(usuario)
                .diasAtras(8)
                .constroi();

        Leilao leilao7Dias = new LeilaoBuilder()
                .comNome("Leilão Encerrado 2")
                .comDono(usuario)
                .diasAtras(9)
                .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilao2Dias);
        this.leilaoDao.salvar(leilao8Dias);
        this.leilaoDao.salvar(leilao7Dias);

        List<Leilao> leiloesAntigos = this.leilaoDao.antigos();

        assertEquals(2L, leiloesAntigos.size());
    }

    @Test
    public void deveTrazerLeiloesNaoEncerradosDentroDoPeriodo() {
         var peiriodoInicio = Calendar.getInstance();
         peiriodoInicio.add(Calendar.DAY_OF_MONTH, -10);

        var periodoFim = Calendar.getInstance();

         var usuario = new Usuario("Usuario 1", "usuario1@email.com");

         var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                 .comDono(usuario)
                .comValor(700.0)
                .diasAtras(2)
                 .constroi();

         var leilao2 = new LeilaoBuilder()
                 .comNome("Computador")
                 .comDono(usuario)
                 .comValor(800.0)
                 .diasAtras(20)
                 .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var leiloes = this.leilaoDao.porPeriodo(peiriodoInicio, periodoFim);

        assertEquals(1L, leiloes.size());
        assertEquals("TV", leiloes.get(0).getNome());
    }

    @Test
    public void naoDeveTrazerLeiloesEncerradosDentroDoIntervalo() {
        var peiriodoInicio = Calendar.getInstance();
        peiriodoInicio.add(Calendar.DAY_OF_MONTH, -10);

        var periodoFim = Calendar.getInstance();

        var usuario = new Usuario("Usuario 1", "usuario1@email.com");

        var leilao1 = new Leilao("TV", 700.0, usuario, false);
        leilao1.setDataAbertura(Calendar.getInstance());
        leilao1.getDataAbertura().add(Calendar.DAY_OF_MONTH, -2);
        leilao1.encerra();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilao1);

        var leiloes = this.leilaoDao.porPeriodo(peiriodoInicio, periodoFim);

        assertEquals(0L, leiloes.size());
    }

    @Test
    public void deveTrazerLeiloesCom3LanchesDisputadosEntreValor() {
        var dono = new Usuario("Dono", "dono@email.com");

        var usuario1 = new Usuario("usuario1", "usuario1@email.com");
        var usuario2 = new Usuario("usuario2", "usuario2@email.com");

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(dono)
                .comValor(800.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 800.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 900.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 1000.0, leilao1));


        var leilao2 = new LeilaoBuilder()
                .comNome("Notebook")
                .comDono(dono)
                .comValor(5000.0)
                .constroi();

        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 5000.0, leilao2));
        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 6000.0, leilao2));
        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 7000.0, leilao2));

        this.usuarioDao.salvar(dono);
        this.usuarioDao.salvar(usuario1);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var leiloes = this.leilaoDao.disputadosEntre(700.0, 1000.0);

        assertEquals(1L, leiloes.size());
        assertEquals("TV", leiloes.get(0).getNome());
    }

    @Test
    public void deveTrazerLeiloesDisputadosEntreValorECom3Laches() {
        var dono = new Usuario("Dono", "dono@email.com");

        var usuario1 = new Usuario("usuario1", "usuario1@email.com");
        var usuario2 = new Usuario("usuario2", "usuario2@email.com");

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(dono)
                .comValor(800.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 800.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 900.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 1000.0, leilao1));


        var leilao2 = new LeilaoBuilder()
                .comNome("Notebook")
                .comDono(dono)
                .comValor(600.0)
                .constroi();

        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 600.0, leilao2));
        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 700.0, leilao2));

        this.usuarioDao.salvar(dono);
        this.usuarioDao.salvar(usuario1);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var leiloes = this.leilaoDao.disputadosEntre(700.0, 900.0);

        assertEquals(1L, leiloes.size());
        assertEquals("TV", leiloes.get(0).getNome());
    }

    @Test
    public void naoDeveTazerLeiloesQuandoNaoExistirLeiloesEntreValorECom3Laces() {
        var dono = new Usuario("Dono", "dono@email.com");

        var usuario1 = new Usuario("usuario1", "usuario1@email.com");
        var usuario2 = new Usuario("usuario2", "usuario2@email.com");

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(dono)
                .comValor(1000.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 1000.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 2000.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 3000.0, leilao1));


        var leilao2 = new LeilaoBuilder()
                .comNome("Notebook")
                .comDono(dono)
                .comValor(600.0)
                .constroi();

        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario1, 600.0, leilao2));
        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 700.0, leilao2));

        this.usuarioDao.salvar(dono);
        this.usuarioDao.salvar(usuario1);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var leiloes = this.leilaoDao.disputadosEntre(700.0, 900.0);

        assertEquals(0L, leiloes.size());
    }

    @Test
    public void deveObterListaDeLeiloesDistintosEmQueUmUsuarioDeuPeloMenosUmLance() {
        var usuario = new Usuario("usuario1", "usuario1@email.com");
        Usuario usuario2 = new Usuario("usuario2", "usuario2@email.com");

        // Leilao 1

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(usuario)
                .comValor(800.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario, 800.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 900.0, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario, 1000.0, leilao1));

        // Leilão 2
        var leilao2 = new LeilaoBuilder()
                .comNome("Geladeira")
                .comDono(usuario)
                .comValor(900.0)
                .constroi();

        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 900.0, leilao2));
        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario, 1000.0, leilao2));

        this.usuarioDao.salvar(usuario);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var leiloes = this.leilaoDao.listaLeiloesDoUsuario(usuario);

        assertEquals(2L, leiloes.size());
    }

    @Test
    public void quandoUsuarioNuncaDeuLancesEntaoObtemListaLeiloesDistintosDoUsuarioVazia() {
        var usuario = new Usuario("usuario1", "usuario1@email.com");
        Usuario usuario2 = new Usuario("usuario2", "usuario2@email.com");

        // Leilao 1

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(usuario)
                .comValor(800.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 900.0, leilao1));

        this.usuarioDao.salvar(usuario);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);

        var leiloes = this.leilaoDao.listaLeiloesDoUsuario(usuario);

        assertEquals(0L, leiloes.size());
    }

    @Test
    public void verificaValorMedioDosLeiloesQueUsuarioParticipa() {
        var usuario = new Usuario("usuario1", "usuario1@email.com");

        var usuario2 = new Usuario("usuario2", "usuario2@email.com");

        var leilao1 = new LeilaoBuilder()
                .comNome("TV")
                .comDono(usuario)
                .comValor(500.0)
                .constroi();

        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario, 500, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario2, 600, leilao1));
        leilao1.adicionaLance(new Lance(Calendar.getInstance(), usuario, 700, leilao1));

        var leilao2 = new LeilaoBuilder()
                .comNome("Mesa")
                .comDono(usuario)
                .comValor(600.0)
                .constroi();

        leilao2.adicionaLance(new Lance(Calendar.getInstance(), usuario, 600, leilao2));

        this.usuarioDao.salvar(usuario);
        this.usuarioDao.salvar(usuario2);
        this.leilaoDao.salvar(leilao1);
        this.leilaoDao.salvar(leilao2);

        var media = this.leilaoDao.getValorInicialMedioDoUsuario(usuario);

        assertEquals((500.0 + 600.0)/2, media, 0.000001);
    }

    @Test
    public void deveDeletarUmLeilao() {
        Usuario usuario = new Usuario("usuario", "usuario@email.com");

        Leilao leilao = new LeilaoBuilder()
                .comNome("TV")
                .comDono(usuario)
                .comValor(800.0)
                .constroi();

        this.usuarioDao.salvar(usuario);
        this.leilaoDao.salvar(leilao);

        this.leilaoDao.deleta(leilao);

        this.session.flush();
        this.session.clear();

        assertNull(this.leilaoDao.porId(leilao.getId()));
    }
}
