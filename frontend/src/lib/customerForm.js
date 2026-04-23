export function createEmptyForm() {
  return {
    name: "",
    dateOfBirth: "",
    nic: "",
    mobileNumbers: [""],
    addresses: [{ addressLine1: "", addressLine2: "", cityId: "", countryId: "" }],
    familyMemberIds: [],
  };
}

export function mapCustomerToForm(customer) {
  return {
    name: customer.name,
    dateOfBirth: customer.dateOfBirth,
    nic: customer.nic,
    mobileNumbers: customer.mobileNumbers.length ? customer.mobileNumbers : [""],
    addresses: customer.addresses.length
      ? customer.addresses.map((address) => ({
          addressLine1: address.addressLine1 || "",
          addressLine2: address.addressLine2 || "",
          cityId: address.cityId ? String(address.cityId) : "",
          countryId: address.countryId ? String(address.countryId) : "",
        }))
      : [{ addressLine1: "", addressLine2: "", cityId: "", countryId: "" }],
    familyMemberIds: customer.familyMembers.map((member) => member.id),
  };
}

export function mapFormToPayload(form) {
  return {
    ...form,
    mobileNumbers: form.mobileNumbers.map((mobile) => mobile.trim()).filter(Boolean),
    addresses: form.addresses
      .map((address) => ({
        addressLine1: address.addressLine1.trim(),
        addressLine2: address.addressLine2.trim(),
        cityId: address.cityId ? Number(address.cityId) : null,
        countryId: address.countryId ? Number(address.countryId) : null,
      }))
      .filter(
        (address) => address.addressLine1 || address.addressLine2 || address.cityId || address.countryId
      ),
  };
}

export function updateForm(setForm, field, value) {
  setForm((current) => ({ ...current, [field]: value }));
}

export function updateMobile(setForm, index, value) {
  setForm((current) => {
    const mobileNumbers = [...current.mobileNumbers];
    mobileNumbers[index] = value;
    return { ...current, mobileNumbers };
  });
}

export function addMobile(setForm) {
  setForm((current) => ({ ...current, mobileNumbers: [...current.mobileNumbers, ""] }));
}

export function removeMobile(setForm, index) {
  setForm((current) => {
    const mobileNumbers = current.mobileNumbers.filter((_, itemIndex) => itemIndex !== index);
    return { ...current, mobileNumbers: mobileNumbers.length ? mobileNumbers : [""] };
  });
}

export function addAddress(setForm) {
  setForm((current) => ({
    ...current,
    addresses: [...current.addresses, { addressLine1: "", addressLine2: "", cityId: "", countryId: "" }],
  }));
}

export function updateAddress(setForm, index, field, value) {
  setForm((current) => ({
    ...current,
    addresses: current.addresses.map((address, itemIndex) =>
      itemIndex === index ? { ...address, [field]: value, ...(field === "countryId" ? { cityId: "" } : {}) } : address
    ),
  }));
}

export function removeAddress(setForm, index) {
  setForm((current) => {
    const addresses = current.addresses.filter((_, itemIndex) => itemIndex !== index);
    return {
      ...current,
      addresses: addresses.length ? addresses : [{ addressLine1: "", addressLine2: "", cityId: "", countryId: "" }],
    };
  });
}

export function toggleFamilyMember(setForm, customerId) {
  setForm((current) => {
    const exists = current.familyMemberIds.includes(customerId);
    return {
      ...current,
      familyMemberIds: exists ? current.familyMemberIds.filter((id) => id !== customerId) : [...current.familyMemberIds, customerId],
    };
  });
}
