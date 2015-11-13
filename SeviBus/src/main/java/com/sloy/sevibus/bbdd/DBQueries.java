package com.sloy.sevibus.bbdd;

import android.util.Log;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.TableUtils;
import com.sloy.sevibus.model.LineaWarning;
import com.sloy.sevibus.model.TweetHolder;
import com.sloy.sevibus.model.tussam.Bonobus;
import com.sloy.sevibus.model.tussam.Favorita;
import com.sloy.sevibus.model.tussam.Linea;
import com.sloy.sevibus.model.tussam.Parada;
import com.sloy.sevibus.model.tussam.ParadaSeccion;
import com.sloy.sevibus.model.tussam.Reciente;
import com.sloy.sevibus.model.tussam.Seccion;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class DBQueries {

    /* -- L√≠neas -- */

    public static Linea getLineaById(DBHelper dbHelper, int id) {
        Linea res = dbHelper.getDaoLinea().queryForId(id);
        return res;
    }

    public static List<Linea> getTodasLineas(DBHelper dbHelper) throws SQLException {
        List<Linea> res = null;

        QueryBuilder<Linea, Integer> queryBuilder = dbHelper.getDaoLinea().queryBuilder();
        queryBuilder.orderBy("numero", true);

        res = queryBuilder.query();
        return res;
    }

    public static List<Linea> getLineasDeParada(DBHelper dbHelper, int parada_id) throws SQLException {
        List<Linea> res = null;

        // Selecciono las relaciones con esta parada
        QueryBuilder<ParadaSeccion, Integer> paradaseccionQb = dbHelper.getDaoParadaSeccion().queryBuilder();
        SelectArg paradaSelectArg = new SelectArg();
        paradaSelectArg.setValue(parada_id);
        paradaseccionQb.where().eq("parada_id", paradaSelectArg);

        // Selecciono las secciones que contienen esta relaci√≥n
        QueryBuilder<Seccion, Integer> seccionQb = dbHelper.getDaoSeccion().queryBuilder();
        seccionQb.join(paradaseccionQb);

        // Selecciono las l√≠neas que contienen dichas secciones
        QueryBuilder<Linea, Integer> lineaQb = dbHelper.getDaoLinea().queryBuilder();
        lineaQb.join(seccionQb);
        lineaQb.orderBy("numero", true);
        lineaQb.distinct();
        res = lineaQb.query();

        return res;
    }

    public static List<Linea> getLineasCercanas(DBHelper dbHelper, double latitud, double longitud) throws SQLException {
        List<Parada> paradasCercanas = getParadasCercanas(dbHelper, latitud, longitud, false);
        Set<Linea> lineas = new HashSet<Linea>();
        for (int i = 0; i < 5 && i < paradasCercanas.size(); i++) {
            List<Linea> l = getLineasDeParada(dbHelper, paradasCercanas.get(i).getNumero());
            lineas.addAll(l);
        }
        List<Linea> res = new ArrayList<Linea>(lineas);
        Collections.sort(res);
        return res;
    }

    /* -- Paradas -- */

    public static Parada getParadaById(DBHelper dbHelper, int paradaId) {
        Parada res = dbHelper.getDaoParada().queryForId(paradaId);
        return res;
    }

    public static List<Parada> getParadasDeLinea(DBHelper dbHelper, int linea_id) throws SQLException {
        //GenericRawResults<String[]> strings = dbHelper.getDaoParada().queryRaw("select * from parada where numero in (select parada_id from paradaseccion where seccion_id in (select id from seccion where linea_id = :linea))", String.valueOf(linea_id));

        QueryBuilder<Seccion, Integer> seccionQb = dbHelper.getDaoSeccion().queryBuilder();
        SelectArg lineaSelectArg = new SelectArg();
        lineaSelectArg.setValue(linea_id);
        seccionQb.where().eq("linea_id", lineaSelectArg);

        QueryBuilder<ParadaSeccion, Integer> paradaSeccionQb = dbHelper.getDaoParadaSeccion().queryBuilder();
        paradaSeccionQb.join(seccionQb);

        // Selecciono las paradas coincidentes con la b√∫squeda anterior
        QueryBuilder<Parada, Integer> paradaQb = dbHelper.getDaoParada().queryBuilder();
        paradaQb.join(paradaSeccionQb);

        return paradaQb.query();
    }

    public static List<Parada> getParadasDeSeccion(DBHelper dbHelper, int seccion_id) throws SQLException {
        // Selecciono las relaciones de la secci√≥n
        QueryBuilder<ParadaSeccion, Integer> paradaSeccionQb = dbHelper.getDaoParadaSeccion().queryBuilder();
        SelectArg seccionSelectArg = new SelectArg();
        seccionSelectArg.setValue(seccion_id);
        paradaSeccionQb.where().eq("seccion_id", seccionSelectArg);

        // Selecciono las paradas coincidentes con la búsqueda anterior
        QueryBuilder<Parada, Integer> paradaQb = dbHelper.getDaoParada().queryBuilder();
        paradaQb.join(paradaSeccionQb);

        return paradaQb.query();
    }

    public static List<Parada> getParadasCercanas(DBHelper dbHelper, double latitud, double longitud, boolean orderByDistance) throws SQLException {

        double margen = 0.003;

        double maxLatitud = latitud + margen;
        double minLatitud = latitud - margen;
        double maxLongitud = longitud + margen;
        double minLongitud = longitud - margen;

        QueryBuilder<Parada, Integer> qb = dbHelper.getDaoParada().queryBuilder();
        Where<Parada, Integer> where = qb.where().lt("latitud", maxLatitud).and().gt("latitud", minLatitud).and().lt("longitud", maxLongitud).and().gt("longitud", minLongitud);
        Log.d("Sevibus", "Query cercanas -> " + where.getStatement());
        List<Parada> res = where.query();

        if (orderByDistance) {
            Collections.sort(res, new ParadaDistanciaComparator(latitud, longitud));
        }
        return res;

    }

    public static List<Favorita> getParadasFavoritas(DBHelper dbHelper) throws SQLException {
        QueryBuilder<Favorita, Integer> favQb = dbHelper.getDaoFavorita().queryBuilder();
        favQb.orderBy("orden", true);
        return favQb.query();
    }

    public static Favorita getFavoritaById(DBHelper dbHelper, Integer id) throws SQLException {
        QueryBuilder<Favorita, Integer> favQb = dbHelper.getDaoFavorita().queryBuilder();
        List<Favorita> res = favQb.where().eq("paradaAsociada_id", id).query();
        if (res != null && res.size() > 0) {
            return res.get(0);
        } else {
            return null;
        }
    }

    public static Boolean setParadasFavoritas(final DBHelper dbHelper, final List<Favorita> favoritasOrdenadas) throws SQLException {
        Boolean res = false;
        TableUtils.clearTable(dbHelper.getConnectionSource(), Favorita.class);
        res = dbHelper.getDaoFavorita().callBatchTasks(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                for (Favorita f : favoritasOrdenadas) {
                    dbHelper.getDaoFavorita().createOrUpdate(f);
                }
                return true;
            }
        });

        return res;
    }

    public static void setNewParadaFavorita(DBHelper dbHelper, Parada parada, String nombrePropio, int color) {
        int count = (int) dbHelper.getDaoFavorita().countOf();
        Favorita fav = new Favorita();
        fav.setParadaAsociada(parada);
        fav.setNombrePropio(nombrePropio);
        fav.setColor(color);
        fav.setOrden(count + 1);
        dbHelper.getDaoFavorita().createOrUpdate(fav);
    }

    public static void updateParadaFavorita(DBHelper dbHelper, Favorita updatedFavorita) {
        dbHelper.getDaoFavorita().update(updatedFavorita);
    }

    public static List<Parada> getParadasByQuery(DBHelper dbHelper, String query, long limit) throws SQLException {
        SelectArg arg1 = new SelectArg("%" + query + "%");
        SelectArg arg2 = new SelectArg("%" + query + "%");
        QueryBuilder<Parada, Integer> qb = dbHelper.getDaoParada().queryBuilder();
        qb.limit(limit);
        Where<Parada, Integer> where = qb.where().like("numero", arg1).or().like("descripcion", arg2);
        Log.d("Sevibus DB", where.getStatement());

        return where.query();
    }

    public static List<Reciente> getParadasRecientes(DBHelper dbHelper) throws SQLException {
        return dbHelper.getDaoReciente().queryBuilder().orderBy("id", false).query();
    }

    public static void setParadaReciente(DBHelper dbHelper, Reciente reciente) throws SQLException {
        RuntimeExceptionDao<Reciente,Integer> daoReciente = dbHelper.getDaoReciente();
        // Borra las paradas recientes que tengan este id, primero
        DeleteBuilder<Reciente,Integer> delBuilder = daoReciente.deleteBuilder();
        delBuilder.where().eq("paradaAsociada_id", reciente.getParadaAsociada().getNumero());
        delBuilder.delete();

        // Y ahora la guarda
        daoReciente.create(reciente);
    }

    //TODO añadir una función que use esto
    public static void clearParadasRecientes(DBHelper dbHelper) throws SQLException {
        TableUtils.clearTable(dbHelper.getConnectionSource(), Reciente.class);
    }

    /* -- Twitter -- */

    public static List<TweetHolder> getAllTweets(DBHelper dbHelper) throws SQLException {
        QueryBuilder<TweetHolder, Long> queryBuilder = dbHelper.getDaoTweetHolder().queryBuilder();
        queryBuilder.orderBy("fecha", false);
        return queryBuilder.query();
    }

    public static List<TweetHolder> getTweetsFromSevibus(DBHelper dbHelper) throws SQLException {
        List<TweetHolder> res = null;

        QueryBuilder<TweetHolder, Long> tweetsQb = dbHelper.getDaoTweetHolder().queryBuilder();
        tweetsQb.where().like("username", "sevibus");
        tweetsQb.orderBy("fecha", false);
        res = tweetsQb.query();
        return res;
    }

    public static List<TweetHolder> getTweetsFromTussam(DBHelper dbHelper) throws SQLException {
        List<TweetHolder> res = null;
        QueryBuilder<TweetHolder, Long> tweetsQb = dbHelper.getDaoTweetHolder().queryBuilder();
        tweetsQb.where().like("username", "ayto_tussam");
        tweetsQb.orderBy("fecha", false);
        res = tweetsQb.query();
        return res;
    }

    public static void deleteTweetsFromTussam(DBHelper dbHelper) throws SQLException {
        DeleteBuilder<TweetHolder, Long> deleteBuilder = dbHelper.getDaoTweetHolder().deleteBuilder();
        deleteBuilder.where().like("username", "ayto_tussam");
        deleteBuilder.delete();
    }

    public static void deleteTweetsFromSevibus(DBHelper dbHelper) throws SQLException {
        DeleteBuilder<TweetHolder, Long> deleteBuilder = dbHelper.getDaoTweetHolder().deleteBuilder();
        deleteBuilder.where().like("username", "sevibus");
        deleteBuilder.delete();
    }

    public static void saveTweets(DBHelper dbHelper, List<TweetHolder> tweets) {
        // TODO tiene que haber una mejor forma de hacer esto
        for (TweetHolder t : tweets) {
            dbHelper.getDaoTweetHolder().createOrUpdate(t);
        }
    }

    public static List<LineaWarning> getAllLineaWarnings(DBHelper dbHelper) {
        return dbHelper.getDaoLineaWarning().queryForAll();
    }

    public static List<LineaWarning> getLineaWarnings(DBHelper dbHelper, int linea_id) throws SQLException {
        QueryBuilder<LineaWarning, Long> paradaSeccionQb = dbHelper.getDaoLineaWarning().queryBuilder();
        SelectArg seccionSelectArg = new SelectArg();
        seccionSelectArg.setValue(linea_id);
        paradaSeccionQb.where().eq("linea_id", seccionSelectArg);
        return paradaSeccionQb.query();
    }

    public static void saveLineaWarning(DBHelper dbHelper, List<LineaWarning> warnings) {
        for (LineaWarning warning : warnings) {
            dbHelper.getDaoLineaWarning().create(warning);
        }
    }

    public static void clearLineaWarnings(DBHelper dbHelper) throws SQLException {
        DeleteBuilder<LineaWarning, Long> deleteBuilder = dbHelper.getDaoLineaWarning().deleteBuilder();
        deleteBuilder.delete();
    }

    /* -- Bonobús -- */
    public static List<Bonobus> getBonobuses(DBHelper dbHelper) {
        return dbHelper.getDaoBonobus().queryForAll();
    }

    public static void saveBonobus(DBHelper dbHelper, Bonobus bonobus) {
        dbHelper.getDaoBonobus().createOrUpdate(bonobus);
    }

    public static void deleteBonobus(DBHelper dbHelper, Bonobus bonobus) {
        dbHelper.getDaoBonobus().delete(bonobus);
    }

    private static class ParadaDistanciaComparator implements Comparator<Parada> {

        private int mLatitud;
        private int mLongitud;

        private ParadaDistanciaComparator(double latitud, double longitud) {
            mLatitud = (int) (latitud * 1E6);
            mLongitud = (int) (longitud * 1E6);
        }

        @Override
        public int compare(Parada p1, Parada p2) {
            int distanciaP1 = distancia(p1);
            int distanciaP2 = distancia(p2);
            return distanciaP1 - distanciaP2;
        }

        private int distancia(Parada parada) {
            int distanciaX = Math.abs(mLatitud - (int) (parada.getLatitud() * 1E6));
            int distanciaY = Math.abs(mLongitud - (int) (parada.getLongitud() * 1E6));
            return distanciaX + distanciaY;
        }
    }

}