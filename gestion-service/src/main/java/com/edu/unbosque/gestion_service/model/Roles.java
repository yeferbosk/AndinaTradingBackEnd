package com.edu.unbosque.gestion_service.model;


public enum Roles {
    Trader,
    Comisionista ,
    Administrador ,
    AreaLegal,
    JuntaDirectiva;

    /**
     *
     * @param rol
     * @return Regresa el rol por defecto si no encuentra el que deberia estar en el rol
     */
    public static String buscarRol(String rol) {
        for (Roles role : Roles.values()) {
            if (role.toString().equals(rol)) {
                return role.toString();
            } else {
                return Roles.Trader.toString();
            }
        }
        return null;
    }
}