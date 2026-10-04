/**
 * Spanish ordinals before a masculine singular noun ("premio"): primer and tercer lose their -o,
 * so 1.er and 3.er (also 21.er, 13.er...), but 11.º (undécimo) and every other one with .º.
 * On their own, or before a plural, they keep .º: "premios 3.º a 4.º".
 */
export function ordinalBeforeNoun(position: number): string {
  const apocopated = (position % 10 === 1 || position % 10 === 3) && position % 100 !== 11
  return `${position}.${apocopated ? 'er' : 'º'}`
}
