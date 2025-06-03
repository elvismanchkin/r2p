# Visa Direct Request to Pay (R2P) API: P2P Requirements Only

This document summarizes the requirements for P2P (Person-to-Person) use cases based on the Visa Direct Request to Pay API specification. Only P2P is supported in this implementation.

---

## 1. API Roles

- **Outbound APIs**:  
  These must be implemented by the receiving side (your system must expose these endpoints for Visa to call).
  - All endpoints under `/rtx/api/outbound/` are outbound.

- **Visa Endpoints**:  
  These must be called by the client (your system acts as a client, calling Visa's API).

---

## 2. Outbound APIs (To Be Implemented by Receiving Side)

The following endpoints must be implemented by the receiving side:

- `PATCH /rtx/api/outbound/v1/requestToPay/{paymentRequestId}/confirm`
- `POST /rtx/api/outbound/v1/requestToPay/transaction/tag`
- `POST /rtx/api/outbound/v1/requestToPay/{originalPaymentRequestId}/refund`
- `POST /rtx/api/outbound/v1/requestToPay/{paymentRequestId}/cancel`
- `POST /rtx/api/outbound/v1/requestToPay`
- `PATCH /rtx/api/outbound/v1/requestToPay/{paymentRequestId}/amend`
- `POST /rtx/api/outbound/v1/requestToPay/notifications`

---

## 3. Visa Endpoints (To Be Called by Client)

All other endpoints (e.g., under `/rtx/api/v1/`) are Visa endpoints and must be called by your client implementation.

---

## 4. P2P Requirements

**P2P** (Person-to-Person) refers to requests where an individual initiates a payment request to another individual.

#### Required Fields for P2P (in payloads):

- `creditorFirstName` and `creditorLastName`: **Required**. First and last name of the payee.
- `creditorCountry` and `creditorAgentCountry`: **Required**. ISO country codes.
- `creditorAgentId`: **Required**. Payee's agent ID.
- `creditorAlias` and `creditorAliasType`: Optional, but if alias is provided, alias type must be provided.
- `nationalIdentifiers`: Optional.
- `requestReason`: Must include a valid payment purpose and may include references (e.g., invoice ID).
- `paymentRequests`: Must include payer details (see below).

**Payer (Sender) Details:**
- `debtorFirstName` and `debtorLastName`: **Required** for P2P.
- `debtorAlias` and `debtorAliasType`: Required.
- `debtorAgentId`, `debtorCountry`, `debtorAgentCountry`: Required.
- `requestedAmount` and `requestedAmountCurrency`: Required.

#### Example P2P Initiate Request Payload

```json
{
  "product": "VD",
  "useCase": "P2P",
  "requestReason": {
    "references": [
      {
        "referenceType": "INVOICEID",
        "referenceDate": "2023-01-15",
        "referenceValue": "1234567890"
      }
    ],
    "paymentPurpose": "SVCS",
    "message": "For lunch"
  },
  "paymentRequests": [
    {
      "debtorFirstName": "John",
      "debtorLastName": "B.",
      "debtorAlias": "+447709123457",
      "debtorAliasType": "MOBL",
      "debtorAgentId": "VD123445",
      "debtorCountry": "UA",
      "debtorAgentCountry": "UA",
      "requestedAmount": 100,
      "requestedAmountCurrency": "UAH"
    }
  ],
  "dueDate": "2021-03-17",
  "requestMessageId": "GG9983636387737JH",
  "settlementOptions": [
    {
      "settlementSystem": "VISA_DIRECT",
      "primaryAccountNumber": "4145124125553222"
    }
  ]
}
```

#### Validation Notes for P2P

- `creditorFirstName` and `creditorLastName` are **mandatory**.
- `debtorFirstName` and `debtorLastName` are **mandatory**.
- `creditorCountry`, `creditorAgentCountry`, `debtorCountry`, `debtorAgentCountry` are **mandatory**.
- `requestedAmount` and `requestedAmountCurrency` are **mandatory**.

---

## 5. Field Requirements Summary

| Field                   | P2P Required | Notes                                                      |
|-------------------------|:------------:|------------------------------------------------------------|
| creditorFirstName       | Yes          | First name of payee                                        |
| creditorLastName        | Yes          | Last name of payee                                         |
| creditorCountry         | Yes          | ISO country code                                           |
| creditorAgentCountry    | Yes          | ISO country code                                           |
| creditorAgentId         | Yes          | Payee's agent ID                                           |
| creditorAlias/Type      | Optional     | If alias is provided, type is required                     |
| nationalIdentifiers     | Optional     |                                                            |
| debtorFirstName         | Yes          | First name of payer                                        |
| debtorLastName          | Yes          | Last name of payer                                         |
| debtorAlias/Type        | Yes          | Alias and type for payer                                   |
| debtorAgentId           | Yes          | Payer's agent ID                                           |
| debtorCountry           | Yes          | Payer's country                                            |
| debtorAgentCountry      | Yes          | Payer's agent country                                      |
| requestedAmount         | Yes          | Amount requested                                           |
| requestedAmountCurrency | Yes          | Currency                                                   |

---

## 6. References

- All outbound API paths: `/rtx/api/outbound/v1/...`
- All Visa endpoints: `/rtx/api/v1/...`
- For full field details, see the `api_reference.json` and the OpenAPI schema.

---

**Only P2P is supported in this implementation.** 