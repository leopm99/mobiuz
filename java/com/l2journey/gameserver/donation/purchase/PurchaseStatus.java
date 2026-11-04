package com.l2journey.gameserver.donation.purchase;

public enum PurchaseStatus
{
	PENDING("Pendente", "Aguardando confirmação"),
	CREATED("Criada", "Compra criada. Aguardando pagamento"),
	WAITING("Aguardando", "Aguardando pagamento"),
	CANCELED("Cancelada", "Pagamento cancelado"),
	EXPIRED("Expirada", "Prazo de pagamento expirou"),
	COMPLETED("Concluída", "Pagamento recebido e itens entregues"),
	FAILED("Falhou", "Houve um problema no pagamento"),
	REFUNDED("Reembolsado", "Dinheiro devolvido."),
	OFFLINE("Offline", "Player estava offline e não recebeu os itens."),
	HIDDEN("Oculta", "Compra não mais visível para o player"),
	FINISHING("Finalizando", "Realizando checagens finais antes do encerramento");

	private final String _status;
	private final String _desc;

	private PurchaseStatus(String status, String desc)
	{
		_status = status;
		_desc = desc;
	}

	public final String getName()
	{
		return _status;
	}

	public final String getDesc()
	{
		return _desc;
	}
}
