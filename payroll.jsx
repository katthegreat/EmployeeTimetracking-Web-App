import React from "react";

const payrollData = [
  { name: "Alice", hours: 40, rate: 20, period: "June 16–30" },
  { name: "Bob", hours: 32, rate: 22, period: "June 16–30" },
];

function Payroll() {
  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-4">Payroll Summary</h2>
      <table className="w-full border">
        <thead>
          <tr className="bg-gray-200">
            <th className="p-2">Employee</th>
            <th className="p-2">Hours Worked</th>
            <th className="p-2">Hourly Rate</th>
            <th className="p-2">Pay Period</th>
            <th className="p-2">Total Pay</th>
          </tr>
        </thead>
        <tbody>
          {payrollData.map((row, i) => (
            <tr key={i} className="text-center">
              <td className="p-2">{row.name}</td>
              <td className="p-2">{row.hours}</td>
              <td className="p-2">${row.rate}</td>
              <td className="p-2">{row.period}</td>
              <td className="p-2 font-semibold">${row.hours * row.rate}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Payroll;
