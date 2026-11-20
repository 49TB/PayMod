package com.paymentfx.chat;

/** A payment notification recovered from chat. */
public record Payment(String sender, long value, String amountText) {
}
