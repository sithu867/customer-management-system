import {
  addAddress,
  createEmptyForm,
  mapFormToPayload,
  removeMobile,
  updateAddress,
} from "./customerForm";

describe("customer form helpers", () => {
  it("maps form values into the API payload", () => {
    const payload = mapFormToPayload({
      name: "Alice",
      dateOfBirth: "1990-01-01",
      nic: "901234567V",
      mobileNumbers: [" 0771234567 ", " "],
      addresses: [
        {
          addressLine1: "  12 Main Street ",
          addressLine2: "",
          cityId: "3",
          countryId: "2",
        },
      ],
      familyMemberIds: [1],
    });

    expect(payload.mobileNumbers).toEqual(["0771234567"]);
    expect(payload.addresses).toEqual([
      {
        addressLine1: "12 Main Street",
        addressLine2: "",
        cityId: 3,
        countryId: 2,
      },
    ]);
  });

  it("resets the city when the country changes", () => {
    let form = {
      ...createEmptyForm(),
      addresses: [{ addressLine1: "", addressLine2: "", cityId: "9", countryId: "1" }],
    };

    updateAddress((updater) => {
      form = updater(form);
    }, 0, "countryId", "2");

    expect(form.addresses[0]).toEqual({
      addressLine1: "",
      addressLine2: "",
      cityId: "",
      countryId: "2",
    });
  });

  it("keeps at least one mobile field available", () => {
    let form = {
      ...createEmptyForm(),
      mobileNumbers: ["0771234567"],
    };

    removeMobile((updater) => {
      form = updater(form);
    }, 0);

    expect(form.mobileNumbers).toEqual([""]);
  });

  it("adds an extra empty address card", () => {
    let form = createEmptyForm();

    addAddress((updater) => {
      form = updater(form);
    });

    expect(form.addresses).toHaveLength(2);
  });
});
