# Visa Direct Request to Pay (R2P) API: B2B and B2C Requirements

This document summarizes the requirements for B2B and B2C use cases based on the Visa Direct Request to Pay API specification. It also clarifies which APIs must be implemented by the receiving side (outbound APIs) and which must be called by the client (Visa endpoints).

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

## 4. B2B and B2C Requirements

### 4.1. B2C Requirements

**B2C** (Business-to-Consumer) refers to requests where a business initiates a payment request to a consumer.

#### Required Fields for B2C (in payloads):

- `creditorBusinessName`: **Required**. Name of the business entity submitting the R2P request.
- `creditorMcc`: **Required**. Merchant Category Code for the business.
- `creditorCountry` and `creditorAgentCountry`: **Required**. ISO country codes.
- `creditorTaxId`: **Required** for certain countries (e.g., UA, DE).
- `nationalIdentifiers`: Optional in Visa API, but **required where local regulation mandates**.
- `requestReason`: Must include a valid payment purpose and may include references (e.g., invoice ID).
- `paymentRequests`: Must include payer details (see below).

**Payer (Consumer) Details:**
- `debtorFirstName` and `debtorLastName`: Optional for B2C (but required for P2P).
- `debtorAlias` and `debtorAliasType`: Required.
- `debtorAgentId`, `debtorCountry`, `debtorAgentCountry`: Required.
- `requestedAmount` and `requestedAmountCurrency`: Required.

#### Example B2C Initiate Request Payload

```json
{
  "product": "VD",
  "useCase": "B2C",
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

#### Validation Notes for B2C

- `creditorBusinessName` and `creditorMcc` are **mandatory**.
- `creditorTaxId` is **mandatory** for some countries.
- `debtorFirstName` and `debtorLastName` are **optional** for B2C, but may be required by local regulation.
- `nationalIdentifiers` may be required by local regulation.

---

### 4.2. B2B Requirements

**B2B** (Business-to-Business) is not explicitly described in the provided API spec. If B2B is to be supported, it would likely follow the B2C pattern, but with both creditor and debtor being businesses.

**Typical B2B requirements (by analogy to B2C):**
- Both `creditorBusinessName` and `debtorBusinessName` would be required.
- `creditorMcc`, `creditorCountry`, `creditorAgentCountry`, and `creditorTaxId` would be required.
- `debtorBusinessName`, `debtorAgentId`, `debtorCountry`, `debtorAgentCountry`, and `debtorTaxId` would be required.
- All other fields as per B2C.

**Note:**  
If you need to support B2B, confirm with Visa or your business analyst for any additional or different requirements, as the current API spec and examples focus on B2C and P2P.

---

## 5. Field Requirements Summary

| Field                   | B2C Required | B2B Required (assumed) | Notes                                                      |
|-------------------------|:------------:|:----------------------:|------------------------------------------------------------|
| creditorBusinessName    | Yes          | Yes                    | Name of business entity                                    |
| creditorMcc             | Yes          | Yes                    | Merchant Category Code                                     |
| creditorCountry         | Yes          | Yes                    | ISO country code                                           |
| creditorAgentCountry    | Yes          | Yes                    | ISO country code                                           |
| creditorTaxId           | Yes*         | Yes*                   | Required for some countries                                |
| nationalIdentifiers     | Yes*         | Yes*                   | Required where local regulation mandates                   |
| debtorBusinessName      | No           | Yes                    | Required for B2B debtor                                    |
| debtorFirstName/LastName| Optional     | No                     | Optional for B2C, not used for B2B                         |
| debtorAlias/Type        | Yes          | Yes                    | Alias and type for payer                                   |
| debtorAgentId           | Yes          | Yes                    | Payer's agent ID                                           |
| debtorCountry           | Yes          | Yes                    | Payer's country                                            |
| debtorAgentCountry      | Yes          | Yes                    | Payer's agent country                                      |
| requestedAmount         | Yes          | Yes                    | Amount requested                                           |
| requestedAmountCurrency | Yes          | Yes                    | Currency                                                   |

\* = Required only in certain countries or regulatory environments.

---

## 6. References

- All outbound API paths: `/rtx/api/outbound/v1/...`
- All Visa endpoints: `/rtx/api/v1/...`
- For full field details, see the `api_reference.json` and the OpenAPI schema.

---

**If you need further clarification on B2B, consult Visa documentation or your business analyst, as the current API spec is focused on B2C and P2P.** 