package projetoi.meucarro.models;

import java.util.Date;


public class GastoCombustivel extends Gasto {

    public float valorUnidade;

    public GastoCombustivel(String descricao, Date data, float valor, float valorUnidade) {
        super(descricao, data, valor);
        this.valorUnidade = valorUnidade;
    }

    @Override
    public String toString() {
        return super.toString() + " Litros: " + valorUnidade;
    }
}
