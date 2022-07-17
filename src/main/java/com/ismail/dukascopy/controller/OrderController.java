package com.ismail.dukascopy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dukascopy.api.IEntryOrder;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.ismail.dukascopy.DukasConstants;
import com.ismail.dukascopy.model.ApiException;
import com.ismail.dukascopy.model.ClosePositionResp;
import com.ismail.dukascopy.model.OrderSide;
import com.ismail.dukascopy.model.OrderType;
import com.ismail.dukascopy.model.Position;
import com.ismail.dukascopy.service.DukasStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Submit new orders to the Strategy
 * 
 * @author ismail
 * @since 20220705
 */
@RestController
@Slf4j
public class OrderController {
	@Autowired
	private DukasStrategy strategy;

	/**
	 * Get position detail (aka Dukas Order)
	 * 
	 * @param clientOrderID
	 * @param dukasOrderID
	 * @return
	 */
	@RequestMapping(value = "/api/v1/positions", method = RequestMethod.GET)
	public ArrayList<Position> getPositions() {

		ArrayList<Position> positions = new ArrayList<>();
		try {
			List<IOrder> openOrders = strategy.getPositions();

			openOrders.forEach(order -> {
				Position position = new Position();
				convertOrderToPosition(order, position);
				positions.add(position);
			});

		} catch (Exception e) {
			throw new ApiException(e.getMessage());
		}

		return positions;
	}

	/**
	 * Get position detail (aka Dukas Order)
	 * 
	 * @param clientOrderID
	 * @param dukasOrderID
	 * @return
	 */
	@RequestMapping(value = "/api/v1/position", method = RequestMethod.GET)
	public Position getPosition(@RequestParam(required = false) Optional<String> clientOrderID,
			@RequestParam(required = false) Optional<String> dukasOrderID) {

		Position position = new Position();
		String clientId = clientOrderID.orElse(null);
		String dukasId = dukasOrderID.orElse(null);
		position.setClientOrderID(clientId);
		position.setDukasOrderID(dukasId);

		try {

			IOrder order = strategy.getPosition(clientId, dukasId);

			convertOrderToPosition(order, position);

			position.setValid(true);
		} catch (Exception e) {
			log.error("getPosition() error: ", e.getMessage(), e);

			position.setErrorMsg(e.getMessage());
			position.setValid(false);
		}

		return position;
	}

	/**
	 * @param clientOrderID
	 * @param instID
	 * @param buySide
	 * @param orderType
	 * @param quantity
	 * @param price         optional: default 0.0
	 * @param slippage      optional; default 5.0 pips
	 * @return
	 */
	@RequestMapping(value = "/api/v1/position", method = RequestMethod.POST)
	public Position openPosition(
			@RequestParam String clientOrderID,
			@RequestParam String instID,
			@RequestParam String orderSide,
			@RequestParam String orderType,
			@RequestParam double quantity,
			@RequestParam(required = false, defaultValue = "0.0") String price,
			@RequestParam(required = false, defaultValue = "0.0") String slippage) {

		Instrument instrument = Instrument.valueOf(instID);
		log.info("instrument: " + instrument);
		if (instrument == null)
			throw new ApiException("Invalid instrument: " + instID);

		Position position = new Position();
		position.setClientOrderID(clientOrderID);
		position.setSymbol(instID);

		try {
			long timeout = 5000;

			IOrder order = strategy.openPosition(clientOrderID,
					instrument,
					OrderSide.valueOf(orderSide),
					OrderType.valueOf(orderType),
					quantity,
					Double.parseDouble(price),
					Double.parseDouble(slippage),
					timeout);

			if (order != null) {
				convertOrderToPosition(order, position);

				// If canceled; means it was rejected
				if (order.getState() == IOrder.State.CANCELED) {
					position.setValid(false);
					position.setErrorMsg("Order rejected");
				} else {
					position.setValid(true);

				}
			}
		} catch (Exception e) {
			log.error("submitOrder() error: ", e.getMessage(), e);

			position.setErrorMsg(e.getMessage());
			position.setValid(false);

			// throw new ApiException("Server error: " + e.getMessage());
		}

		return position;
	}

	/**
	 * @param clientOrderID
	 * @param takeProfitPips
	 * @param stopLossPips
	 * @return
	 */
	@RequestMapping(value = "/api/v1/position", method = RequestMethod.PUT)
	public Position editPosition(
			@RequestParam(required = false) Optional<String> clientOrderID,
			@RequestParam(required = false) Optional<String> dukasOrderID,
			@RequestParam(required = false, defaultValue = "0.0") String takeProfitPips,
			@RequestParam(required = false, defaultValue = "0.0") String stopLossPips) {

		Position position = new Position();

		if (dukasOrderID.isEmpty() && clientOrderID.isEmpty()) {
			position.setErrorMsg("Either dukasOrderID or clientOrderID are mandatory");
			position.setValid(false);
			return position;
		}

		try {
			long timeout = 5000;

			IOrder order = strategy.editPosition(clientOrderID,
					dukasOrderID,
					Double.parseDouble(takeProfitPips),
					Double.parseDouble(stopLossPips),
					timeout);

			if (order != null) {
				position.setClientOrderID(order.getLabel());
				position.setSymbol(order.getInstrument().name());

				convertOrderToPosition(order, position);

				// If canceled; means it was rejected
				if (order.getState() == IOrder.State.CANCELED) {
					position.setValid(false);
					position.setErrorMsg("Order rejected");
				} else {
					position.setValid(true);

				}
			}
		} catch (Exception e) {
			log.error("editPosition() error: ", e.getMessage(), e);

			position.setErrorMsg(e.getMessage());
			position.setValid(false);

			// throw new ApiException("Server error: " + e.getMessage());
		}

		return position;
	}

	/**
	 * Either clientOrderID or dukasOrderID are mandatory
	 * 
	 * @param dukasOrderID
	 * @param clientOrderID optional
	 * @param quantity      optional. Defaults to order quantity
	 * @param price         optional: default to 0.0 (market order to close
	 *                      position)
	 * @param slippage      optional; default 5.0 pips
	 * @return
	 * @throws JFException
	 */
	@RequestMapping(value = "/api/v1/position", method = RequestMethod.DELETE)
	public ClosePositionResp closePosition(@RequestParam Optional<String> dukasOrderID,
			@RequestParam(required = false) Optional<String> clientOrderID,
			@RequestParam(required = false) String quantity,
			@RequestParam(required = false) String price,
			@RequestParam(required = false) String slippage) {

		ClosePositionResp resp = new ClosePositionResp();

		if (dukasOrderID.isEmpty() && clientOrderID.isEmpty()) {
			resp.setRejectReason("Either dukasOrderID or clientOrderID are mandatory");
			resp.setCloseSuccess(false);
			return resp;
		}

		try {
			long timeout = 5000;

			IOrder order = strategy.closePosition(clientOrderID, dukasOrderID,
					Double.parseDouble(quantity),
					Double.parseDouble(price),
					Double.parseDouble(slippage), timeout);

			resp.setOrder(order);
			resp.setCloseSuccess(true);

			return resp;
		} catch (Exception e) {
			log.error("closePosition() error: ", e.getMessage(), e);

			resp.setRejectReason(e.getMessage());
			resp.setCloseSuccess(false);

		}

		return resp;
	}

	private Position convertOrderToPosition(IOrder order, Position pos) {

		pos.setDukasOrderID(order.getId());
		pos.setClientOrderID(order.getLabel());

		pos.setState(order.getState().toString());

		pos.setSymbol(order.getInstrument().toString());

		pos.setCreationTime(order.getCreationTime());

		pos.setQuantity(order.getRequestedAmount() * DukasConstants.lotSize);

		pos.setOpenPrice(order.getOpenPrice());

		if (order.getState() == IOrder.State.FILLED)
			pos.setOpenQuantity(order.getAmount() * DukasConstants.lotSize);

		pos.setClosePrice(order.getClosePrice());

		if (order.getState() == IOrder.State.CLOSED) {
			pos.setCloseQuantity(order.getAmount() * DukasConstants.lotSize);

			pos.setCloseTime(order.getCloseTime());
		}

		pos.setCommission(order.getCommission());

		pos.setBuySide(order.isLong());

		pos.setStopLossPrice(order.getStopLossPrice());
		pos.setTakeProfitPrice(order.getTakeProfitPrice());

		return pos;
	}

}
